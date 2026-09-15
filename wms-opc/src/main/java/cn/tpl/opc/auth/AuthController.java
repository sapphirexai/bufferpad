package cn.tpl.opc.auth;

import cn.tpl.opc.commons.dto.ResultDTO;
import lombok.Data;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;
    private final AuthCookies cookies;
    public AuthController(AuthService service, AuthCookies cookies) { this.service = service; this.cookies = cookies; }

    @GetMapping("/csrf")
    public ResultDTO<?> csrf(CsrfToken token) { return ResultDTO.success(Map.of("token", token.getToken())); }

    @PostMapping("/login")
    public ResultDTO<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String token = service.login(body.username, body.password, request.getRemoteAddr());
        cookies.write(response, token);
        return ResultDTO.success(service.authenticate(token).profile());
    }

    @GetMapping("/me")
    public ResultDTO<?> me(@AuthenticationPrincipal AuthPrincipal principal) { return ResultDTO.success(principal.profile()); }

    @PostMapping("/logout")
    public ResultDTO<?> logout(@AuthenticationPrincipal AuthPrincipal principal, HttpServletResponse response) {
        service.logout(principal); cookies.write(response, null); return ResultDTO.success(true);
    }

    @PostMapping("/password")
    public ResultDTO<?> password(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody PasswordRequest body, HttpServletResponse response) {
        service.changePassword(principal, body.oldPassword, body.newPassword);
        cookies.write(response, null); return ResultDTO.success(true);
    }

    @Data public static class LoginRequest {
        @NotBlank @Size(max=64) private String username;
        @NotBlank @Size(max=64) private String password;
    }
    @Data public static class PasswordRequest {
        @NotBlank @Size(max=64) private String oldPassword;
        @NotBlank @Size(min=8,max=64) private String newPassword;
    }
}
