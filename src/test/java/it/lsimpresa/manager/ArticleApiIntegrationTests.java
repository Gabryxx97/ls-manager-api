package it.lsimpresa.manager;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
class ArticleApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void clearArticles() {
        articleRepository.deleteAll();
    }

    @Test
    void bothRolesCanListArticles() throws Exception {
        saveArticle("Articolo", "Descrizione");

        mockMvc.perform(get("/api/articles")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk());
    }

    @Test
    void standardUserCreatesArticleWithNormalizedName() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "name": "  Bullone    zincato  ",
                                  "description": "  Misura M8  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/articles/\\d+")))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999)))
                .andExpect(jsonPath("$.name").value("Bullone zincato"))
                .andExpect(jsonPath("$.description").value("Misura M8"));

        ArticleJpa saved = articleRepository.findAll().getFirst();
        assertThat(saved.getSearch()).isEqualTo("Bullone zincato Misura M8");
    }

    @Test
    void blankNameIsRejectedAndBlankDescriptionBecomesNull() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"description\":\"test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Il nome è obbligatorio"));

        mockMvc.perform(post("/api/articles")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valido\",\"description\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void listSupportsPaginationSortingAndCaseInsensitiveSearch() throws Exception {
        saveArticle("Zeta", "Componente finale");
        saveArticle("Alpha", "Ricambio Speciale");
        saveArticle("Beta", "Componente intermedio");

        mockMvc.perform(get("/api/articles")
                        .param("page", "1")
                        .param("pageSize", "1")
                        .param("sort", "{\"name\":\"ASC\"}")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/3"))
                .andExpect(jsonPath("$[0].name").value("Alpha"));

        mockMvc.perform(get("/api/articles")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"SPECIALE\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Alpha"));
    }

    @Test
    void updateUsesPathIdAndDeleteReturnsNoContent() throws Exception {
        ArticleJpa article = saveArticle("Vecchio nome", "Vecchia descrizione");

        mockMvc.perform(put("/api/articles/{id}", article.getId())
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"name\":\"Nuovo nome\",\"description\":\"Nuova descrizione\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(article.getId()))
                .andExpect(jsonPath("$.name").value("Nuovo nome"));

        mockMvc.perform(delete("/api/articles/{id}", article.getId())
                        .with(role("USER_ROLE")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(articleRepository.findById(article.getId())).isEmpty();
    }

    @Test
    void missingArticleReturnsNotFoundForReadUpdateAndDelete() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", 999999).with(role("USER_ROLE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Articolo non trovato"));

        mockMvc.perform(put("/api/articles/{id}", 999999)
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Inesistente\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/articles/{id}", 999999)
                        .with(role("USER_ROLE")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserCannotAccessArticles() throws Exception {
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csvImportCreatesValidRowsAndReportsInvalidRows() throws Exception {
        mockMvc.perform(post("/api/articles/import")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"rowNumber": 2, "name": "  Primo   articolo ", "description": "  Uno  "},
                                    {"rowNumber": 3, "name": "   ", "description": "Non valido"},
                                    {"rowNumber": 4, "name": "Secondo articolo", "description": ""}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(3))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Il nome è obbligatorio"));

        assertThat(articleRepository.findAll())
                .extracting(ArticleJpa::getName)
                .containsExactlyInAnyOrder("Primo articolo", "Secondo articolo");
    }

    @Test
    void csvImportValidatesEnvelopeAndRowNumber() throws Exception {
        mockMvc.perform(post("/api/articles/import")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items")
                        .value("La richiesta deve contenere almeno un articolo"));

        mockMvc.perform(post("/api/articles/import")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"rowNumber\":1,\"name\":\"Articolo\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("rowNumber"));
    }

    private ArticleJpa saveArticle(String name, String description) {
        ArticleJpa article = new ArticleJpa();
        article.setName(name);
        article.setDescription(description);
        return articleRepository.saveAndFlush(article);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor role(String role) {
        return user("test-user").authorities(new SimpleGrantedAuthority(role));
    }
}
