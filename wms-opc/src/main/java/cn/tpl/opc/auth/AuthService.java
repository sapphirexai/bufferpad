package cn.tpl.opc.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;
    private final LoginAttempts attempts;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash = passwords.encode("invalid-account-password");
    @Value("${auth.bootstrap.username:admin}") private String bootstrapUsername;
    @Value("${auth.bootstrap.password:example-admin-password}") private String bootstrapPassword;

    public AuthService(JdbcTemplate jdbc, TransactionTemplate transactions,
                       ApplicationEventPublisher events, LoginAttempts attempts) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.events = events;
        this.attempts = attempts;
    }

    @PostConstruct
    public void initialize() {
        new ResourceDatabasePopulator(new ClassPathResource("db/auth-schema.sql"),
                new ClassPathResource("db/auth-first-login-policy.sql")).execute(Objects.requireNonNull(jdbc.getDataSource()));
        if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class) == 0) {
            validateUsername(bootstrapUsername);
            rejectReservedUsername(bootstrapUsername);
            validatePassword(bootstrapPassword);
            jdbc.update("INSERT INTO sys_user(username,password_hash,role,enabled,must_change_password) VALUES(?,?,'ADMIN',TRUE,FALSE)",
                    bootstrapUsername, passwords.encode(bootstrapPassword));
            audit("system", "INITIALIZE_ADMIN", bootstrapUsername);
        }
    }

    public String login(String username, String password, String remoteAddress) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 64 || password == null || password.length() > 64) {
            throw new AuthException(401, "用户名或密码错误");
        }
        attempts.check(normalized, remoteAddress);
        // Lock the account through password verification/session creation so a concurrent
        // password reset or disable cannot race a successful login into a valid session.
        String token = transactions.execute(status -> {
            if (BuiltInAdministrator.USERNAME.equals(normalized)) {
                if (!BuiltInAdministrator.matches(password, passwords)) return null;
                String issued = randomToken();
                jdbc.update("INSERT INTO sys_builtin_session(token_hash) VALUES(?)", builtInSessionHash(hash(issued)));
                audit(normalized, "LOGIN", normalized);
                return issued;
            }
            List<Map<String, Object>> users = jdbc.queryForList("SELECT * FROM sys_user WHERE username=? FOR UPDATE", normalized);
            Map<String, Object> user = users.isEmpty() ? null : users.get(0);
            boolean matches = passwords.matches(password, user == null ? dummyHash : (String) user.get("password_hash"));
            if (user == null || !matches || !truth(user.get("enabled"))) return null;
            String issued = randomToken();
            jdbc.update("INSERT INTO sys_user_session(token_hash,user_id) VALUES(?,?)", hash(issued), user.get("id"));
            audit(normalized, "LOGIN", normalized);
            return issued;
        });
        if (token == null) {
            attempts.failed(normalized, remoteAddress);
            audit(normalized, "LOGIN_FAILED", normalized);
            throw new AuthException(401, "用户名或密码错误");
        }
        attempts.succeeded(normalized);
        return token;
    }

    public AuthPrincipal authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        String digest = hash(token);
        List<AuthPrincipal> users = jdbc.query("SELECT u.id,u.username,u.role,u.must_change_password FROM sys_user_session s JOIN sys_user u ON u.id=s.user_id WHERE s.token_hash=? AND u.enabled=TRUE AND u.id>0 AND u.username<>?",
                (rs, row) -> new AuthPrincipal(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), digest, false), digest, BuiltInAdministrator.USERNAME);
        if (!users.isEmpty()) return users.get(0);
        String builtInDigest = builtInSessionHash(digest);
        return isBuiltInSession(builtInDigest) ? BuiltInAdministrator.principal(builtInDigest) : null;
    }

    public boolean isSessionValid(String digest) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_session s JOIN sys_user u ON u.id=s.user_id WHERE s.token_hash=? AND u.enabled=TRUE AND u.id>0 AND u.username<>?", Long.class, digest, BuiltInAdministrator.USERNAME) > 0 || isBuiltInSession(digest);
    }

    // Namespace the credential generation: pre-rename cookies must never gain the new identity.
    private String builtInSessionHash(String digest) {
        return hash("builtin:superadmin:v1:" + digest);
    }

    private boolean isBuiltInSession(String digest) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_builtin_session WHERE token_hash=?", Long.class, digest) > 0;
    }

    public void logout(AuthPrincipal principal) {
        jdbc.update(principal.isBuiltIn() ? "DELETE FROM sys_builtin_session WHERE token_hash=?" : "DELETE FROM sys_user_session WHERE token_hash=?", principal.getSessionHash());
        events.publishEvent(new AuthSessionsRevoked(List.of(principal.getSessionHash())));
        audit(principal.getUsername(), "LOGOUT", principal.getUsername());
    }

    public void changePassword(AuthPrincipal principal, String oldPassword, String newPassword) {
        if (principal.isBuiltIn()) throw new AuthException(403, "超级管理员为系统内置账户，密码不可修改");
        validatePassword(newPassword);
        List<String> revoked = transactions.execute(status -> {
            Map<String, Object> user = accountForUpdate(principal.getId());
            if (!isSessionValid(principal.getSessionHash())) throw new AuthException(401, "登录已失效，请重新登录");
            if (oldPassword == null || oldPassword.length() > 64 || !passwords.matches(oldPassword, (String) user.get("password_hash"))) {
                throw new AuthException(400, "原密码不正确");
            }
            if (passwords.matches(newPassword, (String) user.get("password_hash"))) throw new AuthException(400, "新密码不能与原密码相同");
            jdbc.update("UPDATE sys_user SET password_hash=?,must_change_password=FALSE,updated_at=CURRENT_TIMESTAMP WHERE id=?", passwords.encode(newPassword), principal.getId());
            audit(principal.getUsername(), "CHANGE_PASSWORD", principal.getUsername());
            return revoke(principal.getId());
        });
        events.publishEvent(new AuthSessionsRevoked(revoked));
    }

    public List<Map<String, Object>> users() {
        return jdbc.query("SELECT id,username,role,enabled,must_change_password,created_at FROM sys_user ORDER BY id", (rs, row) -> {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", rs.getLong(1)); user.put("username", rs.getString(2)); user.put("role", rs.getString(3));
            user.put("enabled", rs.getBoolean(4)); user.put("mustChangePassword", rs.getBoolean(5)); user.put("createdAt", rs.getTimestamp(6));
            return user;
        });
    }

    public void createUser(AuthPrincipal actor, String username, String password, String role) {
        validateUsername(username); validatePassword(password); validateRole(role);
        rejectReservedUsername(username);
        transactions.executeWithoutResult(status -> {
            lockAccountsAndCheckActor(actor);
            if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username=?", Long.class, username) > 0) throw new AuthException(400, "用户名已存在");
            jdbc.update("INSERT INTO sys_user(username,password_hash,role,must_change_password) VALUES(?,?,?,FALSE)", username, passwords.encode(password), role);
            audit(actor.getUsername(), "CREATE_USER", username);
        });
    }

    public void updateUser(AuthPrincipal actor, long id, String role, boolean enabled) {
        validateRole(role);
        List<String> revoked = transactions.execute(status -> {
            lockAccountsAndCheckActor(actor);
            Map<String, Object> target = accountForUpdate(id);
            if ("ADMIN".equals(target.get("role")) && truth(target.get("enabled")) && (!enabled || !"ADMIN".equals(role))
                    && jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE role='ADMIN' AND enabled=TRUE", Long.class) <= 1) {
                throw new AuthException(400, "至少需要保留一个启用的管理员");
            }
            if (role.equals(target.get("role")) && enabled == truth(target.get("enabled"))) return List.<String>of();
            jdbc.update("UPDATE sys_user SET role=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", role, enabled, id);
            audit(actor.getUsername(), "UPDATE_USER", (String) target.get("username"));
            return revoke(id);
        });
        events.publishEvent(new AuthSessionsRevoked(revoked));
    }

    public void resetPassword(AuthPrincipal actor, long id, String password) {
        validatePassword(password);
        List<String> revoked = transactions.execute(status -> {
            lockAccountsAndCheckActor(actor);
            Map<String, Object> target = accountForUpdate(id);
            jdbc.update("UPDATE sys_user SET password_hash=?,must_change_password=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?", passwords.encode(password), id);
            audit(actor.getUsername(), "RESET_PASSWORD", (String) target.get("username"));
            return revoke(id);
        });
        events.publishEvent(new AuthSessionsRevoked(revoked));
    }

    private void lockAccountsAndCheckActor(AuthPrincipal actor) {
        // All account administration acquires the same ordered locks; protects the last
        // administrator even when two administrators demote/disable each other concurrently.
        jdbc.queryForList("SELECT id FROM sys_user ORDER BY id FOR UPDATE");
        if (actor.isBuiltIn()) {
            if (actor.getId() != BuiltInAdministrator.ID || !BuiltInAdministrator.USERNAME.equals(actor.getUsername())
                    || !"ADMIN".equals(actor.getRole()) || !isBuiltInSession(actor.getSessionHash())) {
                throw new AuthException(403, "登录已失效，请重新登录");
            }
            return;
        }
        Map<String, Object> current = accountForUpdate(actor.getId());
        if (!"ADMIN".equals(current.get("role")) || !truth(current.get("enabled")) || !isSessionValid(actor.getSessionHash())) {
            throw new AuthException(403, "权限不足，仅管理员可执行此操作");
        }
    }

    public void recoverAdministrator(String username, String password) {
        validateUsername(username); validatePassword(password);
        rejectReservedUsername(username);
        List<String> revoked = transactions.execute(status -> {
            jdbc.queryForList("SELECT id FROM sys_user ORDER BY id FOR UPDATE");
            List<Long> ids = jdbc.queryForList("SELECT id FROM sys_user WHERE username=? AND role='ADMIN'", Long.class, username);
            if (ids.isEmpty()) throw new AuthException(400, "指定的管理员账户不存在");
            jdbc.update("UPDATE sys_user SET enabled=TRUE,password_hash=?,must_change_password=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?", passwords.encode(password), ids.get(0));
            audit("local-maintenance", "RECOVER_ADMIN", username);
            return revoke(ids.get(0));
        });
        events.publishEvent(new AuthSessionsRevoked(revoked));
    }

    private Map<String, Object> accountForUpdate(long id) {
        if (id == BuiltInAdministrator.ID) throw new AuthException(403, "超级管理员为系统内置账户，不可修改");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM sys_user WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new AuthException(404, "用户不存在");
        rejectReservedUsername((String) rows.get(0).get("username"));
        return rows.get(0);
    }

    private void rejectReservedUsername(String username) {
        if (BuiltInAdministrator.USERNAME.equalsIgnoreCase(username)) {
            throw new AuthException(403, "superadmin为系统内置账户，不可创建或修改");
        }
    }

    private List<String> revoke(long userId) {
        List<String> hashes = jdbc.queryForList("SELECT token_hash FROM sys_user_session WHERE user_id=?", String.class, userId);
        jdbc.update("DELETE FROM sys_user_session WHERE user_id=?", userId);
        return hashes;
    }

    private void audit(String actor, String action, String target) {
        jdbc.update("INSERT INTO sys_auth_audit(actor,action,target_username) VALUES(?,?,?)", actor, action, target);
    }

    private boolean truth(Object value) { return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number) value).intValue() != 0); }
    private void validateUsername(String value) {
        if (value == null || !value.matches("[a-z][a-z0-9_.-]{2,31}")) throw new AuthException(400, "用户名须为3–32位小写字母、数字、下划线、点或横线，以字母开头");
    }
    private void validateRole(String value) {
        if (!"USER".equals(value) && !"ADMIN".equals(value)) throw new AuthException(400, "用户角色不正确");
    }
    private void validatePassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 64 || value.getBytes(StandardCharsets.UTF_8).length > 72 || value.isBlank()) {
            throw new AuthException(400, "密码须为8–64个字符，且UTF-8编码不超过72字节");
        }
    }
    private String randomToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
