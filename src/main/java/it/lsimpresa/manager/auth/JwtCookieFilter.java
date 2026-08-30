package it.lsimpresa.manager.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Component
@Slf4j
@AllArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ApplicationUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")
                || "/api/refresh".equals(path)
                || "/api/login".equals(path)
                || "/api/signup".equals(path)
                || "/api/account-verify".equals(path)
                || "/api/password-recovery".equals(path)
                || "/api/reset-password".equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        Cookie access = WebUtils.getCookie(request, "access_token");
        if (access == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Token missing\",\"message\":\"Access token not present (possibly expired)\"}"
            );
            return;
        }
        String token = access.getValue();

        try {
            String username = jwtTokenService.validateAccessTokenAndGetUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);

        } catch (TokenExpiredException ex) {
            log.debug("Access token expired – returning 401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired\",\"message\":\"Access token has expired\"}");


        } catch (JWTVerificationException ex) {
            log.debug("Invalid JWT – returning 403");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"message\":\"Signature or claims invalid\"}");
        }
    }
}