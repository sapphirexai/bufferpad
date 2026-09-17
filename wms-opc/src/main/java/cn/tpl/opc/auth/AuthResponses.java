package cn.tpl.opc.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {AuthController.class, UserController.class})
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class AuthResponses {
    public static Map<String, Object> body(int status, String message, String reason) {
        return Map.of("code", status, "codeSuccess", false, "msg", message, "reason", reason);
    }

    public static void write(HttpServletResponse response, int status, String message, String reason) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), body(status, message, reason));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handle(AuthException exception) {
        return ResponseEntity.status(exception.status).body(body(exception.status, exception.getMessage(), "AUTH_ERROR"));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalidInput(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        // Never include rejected values: the invalid field may contain a password.
        return ResponseEntity.badRequest().body(body(400, "请求参数不正确，请检查必填项及输入长度", "VALIDATION_FAILED"));
    }
}
