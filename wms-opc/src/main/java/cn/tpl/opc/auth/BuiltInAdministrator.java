package cn.tpl.opc.auth;

import org.springframework.security.crypto.password.PasswordEncoder;

/** Fixed maintenance identity. No user row or mutable credential is stored in the database. */
public final class BuiltInAdministrator {
    public static final long ID = -1L;
    public static final String USERNAME = "superadmin";
    private static final String PASSWORD_HASH = "REMOVED_SHARED_PASSWORD_HASH";

    private BuiltInAdministrator() {}

    public static boolean matches(String password, PasswordEncoder encoder) {
        return encoder.matches(password, PASSWORD_HASH);
    }

    public static AuthPrincipal principal(String sessionHash) {
        return new AuthPrincipal(ID, USERNAME, "ADMIN", false, sessionHash, true);
    }
}
