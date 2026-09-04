package it.lsimpresa.manager.configuration;

import it.lsimpresa.manager.auth.JwtCookieFilter;
import it.lsimpresa.manager.exception.ApiProblemWriter;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebSecurity {

    private final JwtCookieFilter jwtCookieFilter;
    private final ApiProblemWriter problemWriter;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/login",
                                "/api/refresh",
                                "/api/csrf",
                                "/",
                                "/index.html",
                                "/background-image.jpg",
                                "/favicon.png",
                                "/logo.png",
                                "/logo-black.png",
                                "/assets/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                problemWriter.write(response, request,
                                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                                        "Autenticazione richiesta"))
                        .accessDeniedHandler((request, response, exception) ->
                                problemWriter.write(response, request,
                                        org.springframework.http.HttpStatus.FORBIDDEN,
                                        "Non hai i permessi per eseguire questa operazione")))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> {
            cookie.path("/")
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite);
            if (cookieDomain != null && !cookieDomain.isBlank()) {
                cookie.domain(cookieDomain);
            }
        });
        return repository;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.addExposedHeader(SearchControllerBase.CONTENT_RANGE_HEADER);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
