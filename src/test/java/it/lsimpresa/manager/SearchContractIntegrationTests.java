package it.lsimpresa.manager;

import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import it.lsimpresa.manager.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchContractIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void createUsers() {
        userRepository.deleteAll();
        userRepository.save(user("zeta", "Mario", "Zeta"));
        userRepository.save(user("alpha", "Anna", "Alpha"));
        userRepository.save(user("beta", "Bruno", "Beta"));
    }

    @Test
    void paginatedListUsesOneBasedPagesAndStandardContentRange() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .param("sort", "{\"username\":\"ASC\"}")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 1-1/3"))
                .andExpect(jsonPath("$[0].username").value("beta"));
    }

    @Test
    void searchIsCaseInsensitiveOnDerivedSearchField() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"ALPHA\"]}}")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].username").value("alpha"));
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("pageSize", "101")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La pagina deve essere maggiore o uguale a 1"));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "101")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("La dimensione pagina deve essere compresa tra 1 e 100"));
    }

    @Test
    void inFilterUsesTheEntityIdField() throws Exception {
        Long alphaId = userRepository.findByUsername("alpha").getId();
        Long betaId = userRepository.findByUsername("beta").getId();

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "100")
                        .param("filter", "{\"id\":{\"operator\":\"IN\",\"values\":["
                                + alphaId + "," + betaId + "]}}")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-1/2"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unknownFilterFieldAndOperatorAreRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("filter", "{\"password\":{\"operator\":\"EQUAL\",\"values\":[\"secret\"]}}")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Campo filtro non consentito: password"));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"EQUAL\",\"values\":[\"alpha\"]}}")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Operatore non consentito per il campo: search"));
    }

    @Test
    void unknownSortFieldAndMalformedJsonAreRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("sort", "{\"password\":\"ASC\"}")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Campo ordinamento non consentito: password"));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("filter", "not-json")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato filtri non valido"));
    }

    private UserJpa user(String username, String name, String surname) {
        UserJpa user = new UserJpa();
        user.setUsername(username);
        user.setPassword("not-used-in-this-test");
        user.setName(name);
        user.setSurname(surname);
        user.setRole(UserRole.OPERATOR_ROLE);
        user.setAccountActive(true);
        return user;
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("admin")
                .authorities(new SimpleGrantedAuthority("ADMIN_ROLE"));
    }
}
