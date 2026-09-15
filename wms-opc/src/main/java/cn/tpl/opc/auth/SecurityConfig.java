package cn.tpl.opc.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService noDefaultPasswordUser() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use the application login endpoint"); };
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService service, AuthCookies cookies,
                                                   CookieCsrfTokenRepository csrf) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .requestCache().disable().formLogin().disable().httpBasic().disable().logout().disable();
        http.cors().and().csrf().csrfTokenRepository(csrf).requireCsrfProtectionMatcher(request ->
                CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
                        || request.getRequestURI().startsWith(request.getContextPath() + "/device/deviceConnections/")
                        || request.getRequestURI().equals(request.getContextPath() + "/device/scannerConnections"));
        http.addFilterBefore(new AuthFilter(service, cookies), CsrfFilter.class);
        http.exceptionHandling()
                .authenticationEntryPoint((request, response, error) -> AuthResponses.write(response, 401, "请先登录或重新登录", "UNAUTHENTICATED"))
                .accessDeniedHandler((request, response, error) -> {
                    boolean csrfError = error instanceof org.springframework.security.web.csrf.CsrfException;
                    AuthResponses.write(response, 403, csrfError ? "请求校验失败，请刷新页面后重试" : "权限不足，仅管理员可执行此操作", csrfError ? "CSRF_INVALID" : "FORBIDDEN");
                });
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/auth/csrf", "/actuator/health").permitAll()
                .antMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .antMatchers("/auth/me", "/auth/password", "/auth/logout").authenticated()
                // Legacy GETs with side effects are deliberately checked BEFORE read-only routes.
                .antMatchers("/device/deviceConnections/**", "/device/scannerConnections").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET,
                        "/cushion/cushionsPage", "/cushion/cushions", "/cushion/cushions/*", "/cushion/detailsPage", "/cushion/details/excel",
                        "/device/devicesPage", "/device/list", "/device/devicesStatus/*", "/device/{id:[0-9]+}",
                        "/deviceInstallPositions", "/deviceInstallPositions/*", "/plcAddr/page", "/plcAddr/{id:[0-9]+}",
                        "/opcConfig", "/opcConfig/page", "/options/deviceTypes", "/options/plcAddrTypes", "/options/deviceInstallPositions", "/options/plcDevices", "/options/scannerDevices",
                        "/scanLogs", "/operationEvents/recent", "/sse/devicesStatus/*").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/cushion/cushions/excel", "/cushion/cushions/excel/time-range").hasAnyRole("USER", "ADMIN")
                .antMatchers("/users", "/users/**", "/cushion/**", "/device/**", "/deviceInstallPositions/**", "/deviceInstallPositions",
                        "/plcAddr", "/plcAddr/**", "/opcConfig", "/opcConfig/**",
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/druid/**", "/actuator/**").hasRole("ADMIN")
                .anyRequest().denyAll();
        return http.build();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokens(@Value("${auth.cookie-secure:false}") boolean secure) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setSecure(secure);
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${auth.allowed-origins:http://localhost:8081,http://127.0.0.1:8081}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Operation-Id", "DeviceType"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Operation-Id"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
