package it.lsimpresa.manager;

import jakarta.servlet.http.Cookie;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import it.lsimpresa.manager.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createAdmin() {
        userRepository.deleteAll();
        UserJpa admin = new UserJpa();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole(UserRole.ADMIN_ROLE);
        admin.setAccountActive(true);
        userRepository.save(admin);
    }

    @Test
    void csrfEndpointInitializesReadableCookie() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void mutationWithoutCsrfIsRejectedWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void validLoginSetsConfiguredAuthenticationCookies() throws Exception {
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().secure("access_token", false))
                .andExpect(cookie().maxAge("access_token", 900))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", false))
                .andExpect(cookie().maxAge("refresh_token", 604800));
    }

    @Test
    void anonymousProtectedRequestIsUnauthorizedWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/users?page=1&pageSize=10"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void standardUserCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users?page=1&pageSize=10")
                        .with(user("standard")
                                .authorities(new SimpleGrantedAuthority("USER_ROLE"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void removedSignupFlowIsNotPublic() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorizedInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/api/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Refresh token missing"));
    }

    @Test
    void logoutExpiresBothAuthenticationCookies() throws Exception {
        mockMvc.perform(post("/api/logout")
                        .with(csrf())
                        .with(user("admin")
                                .authorities(new SimpleGrantedAuthority("ADMIN_ROLE"))))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access_token", ""))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void malformedLoginPayloadReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void invalidAccessTokenReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/me")
                        .cookie(new Cookie("access_token", "not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void missingAuthenticatedRouteReturnsNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/api/does-not-exist")
                        .with(user("admin")
                                .authorities(new SimpleGrantedAuthority("ADMIN_ROLE"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }
}
