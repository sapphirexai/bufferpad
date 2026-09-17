package cn.tpl.opc.auth;

import org.springframework.security.crypto.password.PasswordEncoder;

/** Optional maintenance identity. Its credential is supplied by the deployment, never bundled. */
public final class BuiltInAdministrator {
    public static final long ID = -1L;
    public static final String USERNAME = "superadmin";

    private BuiltInAdministrator() {}

    public static boolean isConfigured(String passwordHash) {
        return passwordHash != null && passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
    }

    public static boolean matches(String password, PasswordEncoder encoder, String passwordHash) {
        return isConfigured(passwordHash) && password != null && encoder.matches(password, passwordHash);
    }

    public static AuthPrincipal principal(String sessionHash) {
        return new AuthPrincipal(ID, USERNAME, "ADMIN", false, sessionHash, true);
    }
}
