package it.lsimpresa.manager;

import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import it.lsimpresa.manager.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bootstrap manuale di un amministratore sul database configurato.
 *
 * Il nome non termina con "Test", quindi questa classe non viene inclusa nella
 * normale esecuzione di `mvn test`. Va richiamata esplicitamente con
 * `-Dtest=AdminUserBootstrap`.
 */
@SpringBootTest
class AdminUserBootstrap {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Autowired
    AdminUserBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void createOrUpdateAdminUser() {
        assertThat(adminUsername)
                .as("ADMIN_USERNAME deve essere valorizzato")
                .isNotBlank();
        assertThat(adminPassword)
                .as("ADMIN_PASSWORD deve essere valorizzata")
                .isNotBlank();

        UserJpa admin = userRepository.findByUsername(adminUsername);
        if (admin == null) {
            admin = new UserJpa();
            admin.setUsername(adminUsername);
        }

        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN_ROLE);
        admin.setAccountActive(true);

        UserJpa savedAdmin = userRepository.saveAndFlush(admin);

        assertThat(savedAdmin.getId()).isNotNull();
        assertThat(savedAdmin.getRole()).isEqualTo(UserRole.ADMIN_ROLE);
        assertThat(savedAdmin.getAccountActive()).isTrue();
        assertThat(passwordEncoder.matches(adminPassword, savedAdmin.getPassword())).isTrue();
    }
}
