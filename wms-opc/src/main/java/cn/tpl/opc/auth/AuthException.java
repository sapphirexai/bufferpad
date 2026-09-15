package cn.tpl.opc.auth;

public class AuthException extends RuntimeException {
    public final int status;
    public AuthException(int status, String message) {
        super(message);
        this.status = status;
    }
}
