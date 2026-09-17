package cn.tpl.opc.auth;

import lombok.Value;

@Value
public class AuthPrincipal {
    long id;
    String username;
    String role;
    boolean mustChangePassword;
    String sessionHash;
    boolean builtIn;

    public java.util.Map<String, Object> profile() {
        return java.util.Map.of("id", id, "username", username, "role", role,
                "mustChangePassword", mustChangePassword, "builtIn", builtIn,
                "passwordChangeAllowed", !builtIn);
    }
}
