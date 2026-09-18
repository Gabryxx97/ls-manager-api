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
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleApiIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ArticleRepository articleRepository;

    @BeforeEach void clearArticles() { articleRepository.deleteAll(); }

    @Test void bothRolesCanListArticles() throws Exception {
        saveArticle("ART-1", "Articolo");
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "10").with(role("ADMIN_ROLE"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "10").with(role("OPERATOR_ROLE"))).andExpect(status().isOk());
    }

    @Test void createNormalizesAndReturnsWarehouseFields() throws Exception {
        mockMvc.perform(post("/api/articles").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"  ab  12 ","description":"  Bullone   zincato  ",
                         "category":" Ferramenta ","costCenter":" Cantiere   A ",
                         "unitOfMeasure":" PZ ","location":" MAG1  A1 ",
                         "stockQuantity":-3.25,"unitPrice":1.20}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value("AB-12"))
                .andExpect(jsonPath("$.description").value("Bullone zincato"))
                .andExpect(jsonPath("$.costCenter").value("Cantiere A"))
                .andExpect(jsonPath("$.stockQuantity").value(-3.25));
        assertThat(articleRepository.findAll().getFirst().getSearch())
                .isEqualTo("AB-12 Bullone zincato Ferramenta Cantiere A PZ MAG1 A1");
    }

    @Test void skuAndDescriptionAreRequiredAndSkuIsCaseInsensitiveUnique() throws Exception {
        mockMvc.perform(post("/api/articles").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\" \",\"description\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").value("Lo SKU è obbligatorio"))
                .andExpect(jsonPath("$.errors.description").value("La descrizione è obbligatoria"));
        saveArticle("ABC", "Primo");
        mockMvc.perform(post("/api/articles").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"abc\",\"description\":\"Secondo\"}"))
                .andExpect(status().isConflict());
    }

    @Test void listSupportsSkuSortingAndSearchesWarehouseFields() throws Exception {
        saveArticle("Z-2", "Finale");
        ArticleJpa alpha = saveArticle("A-1", "Ricambio speciale");
        alpha.setLocation("Furgone 2");
        articleRepository.saveAndFlush(alpha);
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "1")
                        .param("sort", "{\"sku\":\"ASC\"}").with(role("OPERATOR_ROLE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].sku").value("A-1"));
        mockMvc.perform(get("/api/articles").param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"FURGONE\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].sku").value("A-1"));
    }

    @Test void updateUsesPathIdAndDeleteReturnsNoContent() throws Exception {
        ArticleJpa article = saveArticle("OLD", "Vecchia descrizione");
        mockMvc.perform(put("/api/articles/{id}", article.getId()).with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"sku\":\"NEW\",\"description\":\"Nuova descrizione\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(article.getId()))
                .andExpect(jsonPath("$.sku").value("NEW"));
        mockMvc.perform(delete("/api/articles/{id}", article.getId()).with(role("ADMIN_ROLE")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(articleRepository.findById(article.getId())).isEmpty();
    }

    @Test void missingArticleReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", 999999).with(role("OPERATOR_ROLE"))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/articles/{id}", 999999).with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"X\",\"description\":\"Inesistente\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/articles/{id}", 999999).with(role("ADMIN_ROLE")).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void importCreatesUpdatesPreservesPriceAndReportsInvalidRows() throws Exception {
        ArticleJpa existing = saveArticle("EXISTING", "Vecchia");
        existing.setUnitPrice(new BigDecimal("9.50"));
        existing.setCategory("Vecchia categoria");
        articleRepository.saveAndFlush(existing);
        mockMvc.perform(post("/api/articles/import").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"items":[
                          {"rowNumber":2,"sku":"existing","description":"Aggiornata","category":"","stockQuantity":-3.25},
                          {"rowNumber":3,"sku":"NEW-1","description":"Nuovo","unitOfMeasure":"PZ","stockQuantity":1.50},
                          {"rowNumber":4,"sku":"BAD","description":"   "},
                          {"rowNumber":1,"sku":"BAD-ROW","description":"Non valida"}
                        ]}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.received").value(4))
                .andExpect(jsonPath("$.created").value(1)).andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[1].field").value("rowNumber"));
        ArticleJpa updated = articleRepository.findBySkuIgnoreCase("EXISTING").orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("Aggiornata");
        assertThat(updated.getCategory()).isNull();
        assertThat(updated.getStockQuantity()).isEqualByComparingTo("-3.25");
        assertThat(updated.getUnitPrice()).isEqualByComparingTo("9.50");
    }

    @Test void importEnvelopeIsValidated() throws Exception {
        mockMvc.perform(post("/api/articles/import").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").value("La richiesta deve contenere almeno un articolo"));
    }

    private ArticleJpa saveArticle(String sku, String description) {
        ArticleJpa article = new ArticleJpa();
        article.setSku(sku);
        article.setDescription(description == null ? sku : description);
        return articleRepository.saveAndFlush(article);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor role(String role) {
        return user("test-user").authorities(new SimpleGrantedAuthority(role));
    }
}
