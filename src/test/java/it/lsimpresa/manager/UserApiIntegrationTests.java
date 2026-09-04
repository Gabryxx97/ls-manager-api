package it.lsimpresa.manager;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserJpa admin;

    @BeforeEach
    void createAdmin() {
        userRepository.deleteAll();
        admin = saveUser("admin", "admin-password", UserRole.ADMIN_ROLE);
    }

    @Test
    void adminCreatesUserWithHashedPasswordAndPasswordIsNeverReturned() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "username": " new.user@example.test ",
                                  "password": "plain-password",
                                  "role": "USER_ROLE",
                                  "name": "Nuovo",
                                  "surname": "Utente"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/users/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999)))
                .andExpect(jsonPath("$.username").value("new.user@example.test"))
                .andExpect(jsonPath("$.password").doesNotExist());

        UserJpa saved = userRepository.findByUsername("new.user@example.test");
        assertThat(saved).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", saved.getPassword())).isTrue();
    }

    @Test
    void createRequiresUsernamePasswordAndRole() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void updateUsesPathIdAndKeepsPasswordWhenOmitted() throws Exception {
        UserJpa target = saveUser("target", "original-password", UserRole.USER_ROLE);
        String originalHash = target.getPassword();

        mockMvc.perform(put("/api/users/{id}", target.getId())
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "username": "renamed",
                                  "role": "ADMIN_ROLE",
                                  "name": "Nome aggiornato",
                                  "surname": "Cognome aggiornato",
                                  "accountActive": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId()))
                .andExpect(jsonPath("$.username").value("renamed"))
                .andExpect(jsonPath("$.password").doesNotExist());

        UserJpa updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.getPassword()).isEqualTo(originalHash);
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN_ROLE);
    }

    @Test
    void updateHashesANewPassword() throws Exception {
        UserJpa target = saveUser("target", "original-password", UserRole.USER_ROLE);

        mockMvc.perform(put("/api/users/{id}", target.getId())
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "target",
                                  "password": "changed-password",
                                  "role": "USER_ROLE"
                                }
                                """))
                .andExpect(status().isOk());

        UserJpa updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("changed-password", updated.getPassword())).isTrue();
        assertThat(updated.getPassword()).doesNotContain("changed-password");
    }

    @Test
    void duplicateUsernameReturnsConflictOnCreateAndUpdate() throws Exception {
        UserJpa first = saveUser("already-used", "password", UserRole.USER_ROLE);
        UserJpa second = saveUser("second", "password", UserRole.USER_ROLE);

        mockMvc.perform(post("/api/users")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"already-used\",\"password\":\"password\",\"role\":\"USER_ROLE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esiste già un utente con questo username"));

        mockMvc.perform(put("/api/users/{id}", second.getId())
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"already-used\",\"role\":\"USER_ROLE\"}"))
                .andExpect(status().isConflict());

        assertThat(userRepository.findById(first.getId())).isPresent();
        assertThat(userRepository.findById(second.getId()).orElseThrow().getUsername()).isEqualTo("second");
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999999).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Utente non trovato"));

        mockMvc.perform(put("/api/users/{id}", 999999)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing\",\"role\":\"USER_ROLE\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/users/{id}", 999999).with(admin()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotDeleteOwnAccountButCanDeleteAnotherUser() throws Exception {
        UserJpa target = saveUser("target", "password", UserRole.USER_ROLE);

        mockMvc.perform(delete("/api/users/{id}", admin.getId()).with(admin()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Non puoi eliminare il tuo account attivo"));

        mockMvc.perform(delete("/api/users/{id}", target.getId()).with(admin()).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(userRepository.findById(target.getId())).isEmpty();
    }

    @Test
    void standardUserCannotEscalatePrivileges() throws Exception {
        UserJpa standard = saveUser("standard", "password", UserRole.USER_ROLE);

        mockMvc.perform(put("/api/users/{id}", standard.getId())
                        .with(user("standard").authorities(new SimpleGrantedAuthority("USER_ROLE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"standard\",\"role\":\"ADMIN_ROLE\"}"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(standard.getId()).orElseThrow().getRole())
                .isEqualTo(UserRole.USER_ROLE);
    }

    private UserJpa saveUser(String username, String password, UserRole role) {
        UserJpa user = new UserJpa();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountActive(true);
        return userRepository.saveAndFlush(user);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("admin").authorities(new SimpleGrantedAuthority("ADMIN_ROLE"));
    }
}
