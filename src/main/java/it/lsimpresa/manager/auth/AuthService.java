package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.exception.AuthenticationException;
import it.lsimpresa.manager.user.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService{

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.access-token-max-age}")
    private Duration accessCookieMaxAge;

    @Value("${app.cookie.refresh-token-max-age}")
    private Duration refreshCookieMaxAge;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final ApplicationUserDetailsService userDetailsService;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void login (AuthDto auth, HttpServletResponse response) {
        checkUser(auth);
        JwtTokenPair pair = loginToken(auth);
        ResponseCookie accessCookie = buildCookie(
                "access_token", pair.getAccessToken(), "/", accessCookieMaxAge);
        ResponseCookie refreshCookie = buildCookie(
                "refresh_token", pair.getRefreshToken(), "/api/refresh", refreshCookieMaxAge);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    public void refresh (HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = Optional.ofNullable(request.getCookies()).stream()
                .flatMap(java.util.Arrays::stream)
                .filter(c -> "refresh_token".equals(c.getName()))
                .findFirst().map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationException("Refresh token missing"));

        JwtTokenPair newPair = refreshAccessToken(refreshToken);
        ResponseCookie newAccess = buildCookie(
                "access_token", newPair.getAccessToken(), "/", accessCookieMaxAge);
        ResponseCookie newRefresh = buildCookie(
                "refresh_token", newPair.getRefreshToken(), "/api/refresh", refreshCookieMaxAge);
        response.addHeader(HttpHeaders.SET_COOKIE, newAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefresh.toString());
    }

    @Override
    public void logout(HttpServletResponse response) {
        expireCookie(response, "access_token", "/");
        expireCookie(response, "refresh_token", "/api/refresh");
    }

    private JwtTokenPair loginToken(AuthDto auth) {
        Authentication authReq = new UsernamePasswordAuthenticationToken(auth.getUsername(), auth.getPassword());
        authenticationManager.authenticate(authReq);
        UserDetails user = userDetailsService.loadUserByUsername(auth.getUsername());

        String access = jwtTokenService.generateAccessToken(user);
        String refresh = jwtTokenService.generateRefreshToken(user);

        return new JwtTokenPair(access, refresh);
    }

    private JwtTokenPair refreshAccessToken(String refreshToken) {
        String username = jwtTokenService.validateRefreshTokenAndGetUsername(refreshToken);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        String newAccess = jwtTokenService.generateAccessToken(user);
        String newRefresh = jwtTokenService.generateRefreshToken(user);

        return new JwtTokenPair(newAccess, newRefresh);
    }

    private ResponseCookie buildCookie(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(cookieSameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private void expireCookie(HttpServletResponse response, String name, String path) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(name, "", path, Duration.ZERO).toString());
    }

    private void checkUser(AuthDto authDto) {
        UserJpa user = repository.findByUsername(authDto.getUsername());
        if (user == null || !passwordEncoder.matches(authDto.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
