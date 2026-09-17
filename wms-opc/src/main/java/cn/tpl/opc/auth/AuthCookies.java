package cn.tpl.opc.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthCookies {
    public static final String NAME = "BUFFERPAD_SESSION";
    @Value("${auth.cookie-secure:false}") private boolean secure;

    public String read(HttpServletRequest request) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public void write(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, token == null ? "" : token)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/")
                .maxAge(token == null ? 0 : 365L * 24 * 60 * 60).build().toString());
    }
}
