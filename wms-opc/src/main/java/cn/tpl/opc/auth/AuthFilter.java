package cn.tpl.opc.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class AuthFilter extends OncePerRequestFilter {
    private final AuthService service;
    private final AuthCookies cookies;
    private static final Set<String> PUBLIC = Set.of("/auth/login", "/auth/csrf", "/actuator/health");
    private static final Set<String> PASSWORD_ALLOWED = Set.of("/auth/me", "/auth/password", "/auth/logout", "/auth/csrf", "/auth/login");

    public AuthFilter(AuthService service, AuthCookies cookies) { this.service = service; this.cookies = cookies; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String token = cookies.read(request);
        AuthPrincipal principal = service.authenticate(token);
        if (principal != null) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, null, AuthorityUtils.createAuthorityList("ROLE_" + principal.getRole())));
            cookies.write(response, token);
        }
        if (!"OPTIONS".equals(request.getMethod()) && principal == null && !PUBLIC.contains(path)) {
            if (token != null) cookies.write(response, null);
            AuthResponses.write(response, 401, "请先登录或重新登录", "UNAUTHENTICATED");
            return;
        }
        if (principal != null && principal.isMustChangePassword() && !PASSWORD_ALLOWED.contains(path) && !PUBLIC.contains(path)) {
            AuthResponses.write(response, 403, "密码重置后，请先修改密码", "PASSWORD_CHANGE_REQUIRED");
            return;
        }
        chain.doFilter(request, response);
    }
}
