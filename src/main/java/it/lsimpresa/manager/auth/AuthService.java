package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.exception.AuthenticationException;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
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
import org.springframework.web.servlet.view.RedirectView;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static it.lsimpresa.manager.auth.JwtTokenService.ACCESS_TOKEN_VALIDITY;
import static it.lsimpresa.manager.auth.JwtTokenService.REFRESH_TOKEN_VALIDITY;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService{

    @Value("${app.domain.url}")
    private String domainUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final IUserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final ApplicationUserDetailsService userDetailsService;
    private final UserMapper mapper;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void login (AuthDto auth, HttpServletResponse response) {
        checkUser(auth);
        JwtTokenPair pair = loginToken(auth);
        ResponseCookie accessCookie = ResponseCookie.from("access_token", pair.getAccessToken())
                .httpOnly(true).secure(true).path("/")
                .maxAge(ACCESS_TOKEN_VALIDITY.toSeconds())
                .sameSite("Strict")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", pair.getRefreshToken())
                .httpOnly(true).secure(true).path("/api/refresh")
                .maxAge(REFRESH_TOKEN_VALIDITY.toSeconds())
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    @Override
    public void refresh (HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .findFirst().map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationException("Refresh token missing"));

        JwtTokenPair newPair = refreshAccessToken(refreshToken);
        ResponseCookie newAccess = ResponseCookie.from("access_token", newPair.getAccessToken())
                .httpOnly(true).secure(request.isSecure()).path("/")
                .maxAge(ACCESS_TOKEN_VALIDITY.toSeconds())
                .sameSite("Strict")
                .build();
        ResponseCookie newRefresh = ResponseCookie.from("refresh_token", newPair.getRefreshToken())
                .httpOnly(true).secure(request.isSecure()).path("/api/refresh")
                .maxAge(REFRESH_TOKEN_VALIDITY.toSeconds())
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, newAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefresh.toString());
    }

    @Override
    @Transactional
    public UserDto signup(UserDto user) {
        UserJpa existingUser = repository.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new AuthenticationException("Utente già esistente");
        }
        UserJpa entity = mapper.toJpa(user);
        entity.setPassword(passwordEncoder.encode(user.getPassword()));
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public RedirectView verifyAccount(String token) {
        UserJpa user = repository.findByTokenActive(token);
        if (user == null) {
            throw new AuthenticationException("Token di registrazione non valido");
        }
        if (user.getTokenActiveDate().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Token scaduto");
        }
        user.setTokenActive(null);
        user.setTokenActiveDate(null);
        user.setAccountActive(true);
        repository.save(user);
        return new RedirectView(frontendUrl + "/#/thanks");
    }


    @Override
    @Transactional
    public UserDto resetPassword(ResetPasswordDto newPassword) {
        if (newPassword.getToken() == null) {
            throw new ResourceNotFoundException("Invalid token.");
        }
        UserJpa user = repository.findByUsername(newPassword.getUsername());
        validateAndUpdateToken(user, newPassword.getToken());
        user.setPassword(passwordEncoder.encode(newPassword.getPassword()));
        return mapper.toDto(repository.save(user));
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

    private UserJpa generateAndSetPasswordToken(UserJpa user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for token generation.");
        }
        user.setTokenPassword(UUID.randomUUID().toString());
        user.setTokenValidity(LocalDateTime.now());
        user.setTokenUse(false);
        return repository.save(user);
    }

    private void validateAndUpdateToken(UserJpa user, String token) {
        if (Boolean.TRUE.equals(user.getTokenUse())) {
            throw new ResourceNotFoundException("Password reset already performed.");
        }
        if (!isTokenValid(user.getTokenValidity(), LocalDateTime.now())) {
            throw new ResourceNotFoundException("Token expired.");
        }
        if (!token.equals(user.getTokenPassword())) {
            throw new ResourceNotFoundException("Invalid token.");
        }
        user.setTokenUse(true);
    }

    private boolean isTokenValid(LocalDateTime tokenValidityDate, LocalDateTime currentTime) {
        if (tokenValidityDate == null) {
            return false;
        }
        Duration duration = Duration.between(tokenValidityDate, currentTime);
        return duration.toHours() < 24;
    }

    private void checkUser(AuthDto authDto) {
        UserJpa user = repository.findByUsername(authDto.getUsername());
        if (user == null || !passwordEncoder.matches(authDto.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
