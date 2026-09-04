package it.lsimpresa.manager.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import it.lsimpresa.manager.exception.ApiProblemWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ApplicationUserDetailsService userDetailsService;
    private final ApiProblemWriter problemWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/")
                || "/api/login".equals(path)
                || "/api/refresh".equals(path)
                || "/api/csrf".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        Cookie access = WebUtils.getCookie(request, "access_token");
        if (access == null) {
            chain.doFilter(request, response);
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
            problemWriter.write(response, request, HttpStatus.UNAUTHORIZED, "Sessione scaduta");

        } catch (JWTVerificationException ex) {
            log.debug("Invalid JWT – returning 401");
            problemWriter.write(response, request, HttpStatus.UNAUTHORIZED, "Token di accesso non valido");
        }
    }
}
