package it.lsimpresa.manager;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.order.OrderRepository;
import it.lsimpresa.manager.workorder.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
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
class WorkOrderApiIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ArticleRepository articleRepository;

    @BeforeEach
    @AfterEach
    void clearData() {
        orderRepository.deleteAll();
        workOrderRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void crudIsAvailableToBothRolesButNotAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/workorders").param("page", "1").param("pageSize", "10")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/workorders").param("page", "1").param("pageSize", "10")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/workorders").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isUnauthorized());

        long id = createWorkOrder("  Commessa   Alfa  ", "  Descrizione  ", "USER_ROLE");
        mockMvc.perform(get("/api/workorders/{id}", id).with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Commessa Alfa"))
                .andExpect(jsonPath("$.description").value("Descrizione"));

        mockMvc.perform(put("/api/workorders/{id}", id).with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Commessa Beta\",\"description\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Commessa Beta"));

        mockMvc.perform(delete("/api/workorders/{id}", id).with(role("USER_ROLE")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(workOrderRepository.findById(id)).isEmpty();
    }

    @Test
    void validatesNamesAndRejectsNormalizedCaseInsensitiveDuplicates() throws Exception {
        createWorkOrder("Commessa Alfa", null, "ADMIN_ROLE");

        mockMvc.perform(post("/api/workorders").with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  COMMESSA    alfa  \",\"description\":\"Duplicata\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esiste già una commessa con questo nome"));

        mockMvc.perform(post("/api/workorders").with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Il nome è obbligatorio"));

        String tooLong = "a".repeat(256);
        mockMvc.perform(post("/api/workorders").with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Il nome non può superare 255 caratteri"));
    }

    @Test
    void listSupportsSearchSortingAndPagination() throws Exception {
        createWorkOrder("Zeta", "Cliente speciale", "ADMIN_ROLE");
        createWorkOrder("Alfa", "Altro", "ADMIN_ROLE");

        mockMvc.perform(get("/api/workorders")
                        .param("page", "1").param("pageSize", "1")
                        .param("sort", "{\"name\":\"ASC\"}")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/2"))
                .andExpect(jsonPath("$[0].name").value("Alfa"));

        mockMvc.perform(get("/api/workorders")
                        .param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"SPECIALE\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Zeta"));
    }

    @Test
    void renamePropagatesToEveryLinkedOrderAndReferencedDeleteConflicts() throws Exception {
        long workOrderId = createWorkOrder("Commessa iniziale", null, "ADMIN_ROLE");
        ArticleJpa article = new ArticleJpa();
        article.setSku("ARTICOLO");
        article.setDescription("Articolo");
        article.setUnitPrice(new java.math.BigDecimal("1.00"));
        article = articleRepository.saveAndFlush(article);

        long firstOrder = createOrder(workOrderId, article.getId());
        long secondOrder = createOrder(workOrderId, article.getId());

        mockMvc.perform(put("/api/workorders/{id}", workOrderId)
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Commessa rinominata\",\"description\":\"Nuova\"}"))
                .andExpect(status().isOk());

        for (long orderId : new long[]{firstOrder, secondOrder}) {
            mockMvc.perform(get("/api/orders/{id}", orderId).with(role("USER_ROLE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workOrderId").value(workOrderId))
                    .andExpect(jsonPath("$.name").value("Commessa rinominata"));
        }
        mockMvc.perform(get("/api/orders").param("page", "1").param("pageSize", "10")
                        .param("sort", "{\"name\":\"ASC\"}")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"RINOMINATA\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-1/2"));

        mockMvc.perform(delete("/api/workorders/{id}", workOrderId)
                        .with(role("ADMIN_ROLE")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La commessa è utilizzata da uno o più ordini"));
    }

    private long createWorkOrder(String name, String description, String authority) throws Exception {
        String body = description == null
                ? "{\"name\":\"%s\",\"description\":null}".formatted(name)
                : "{\"name\":\"%s\",\"description\":\"%s\"}".formatted(name, description);
        String location = mockMvc.perform(post("/api/workorders").with(role(authority)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long createOrder(long workOrderId, long articleId) throws Exception {
        String location = mockMvc.perform(post("/api/orders").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workOrderId":%d,"date":"2026-09-07","priority":"STANDARD",
                                 "status":"DRAFT","details":[{"articleId":%d,"quantity":1}]}
                                """.formatted(workOrderId, articleId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor role(String role) {
        return user("test-user").authorities(new SimpleGrantedAuthority(role));
    }
}
