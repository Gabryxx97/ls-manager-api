package it.lsimpresa.manager;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.order.OrderJpa;
import it.lsimpresa.manager.order.OrderRepository;
import it.lsimpresa.manager.workorder.WorkOrderJpa;
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
class OrderApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @BeforeEach
    @AfterEach
    void clearData() {
        orderRepository.deleteAll();
        workOrderRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void bothRolesCanListOrdersWhileAnonymousUserCannot() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsDetailedOrderAndListReturnsOnlyHeader() throws Exception {
        ArticleJpa article = saveArticle("Bullone M8", "Zincato");
        WorkOrderJpa workOrder = saveWorkOrder("  Ordine   urgente  ");

        String response = mockMvc.perform(post("/api/orders")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), "2026-09-05", "HIGH",
                                "DRAFT", article.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/orders/\\d+")))
                .andExpect(jsonPath("$.name").value("Ordine urgente"))
                .andExpect(jsonPath("$.workOrderId").value(workOrder.getId()))
                .andExpect(jsonPath("$.date").value("2026-09-05"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.details[0].articleId").value(article.getId()))
                .andExpect(jsonPath("$.details[0].articleName").value("Bullone M8"))
                .andExpect(jsonPath("$.details[0].articleDescription").value("Zincato"))
                .andExpect(jsonPath("$.details[0].quantity").value(3))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("\"id\"");
        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Ordine urgente"))
                .andExpect(jsonPath("$[0].details").doesNotExist());
    }

    @Test
    void getReturnsHeaderAndLines() throws Exception {
        ArticleJpa article = saveArticle("Dado M8", null);
        long orderId = createOrder(article.getId());

        mockMvc.perform(get("/api/orders/{id}", orderId).with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].articleName").value("Dado M8"));
    }

    @Test
    void updateAtomicallyReplacesDetails() throws Exception {
        ArticleJpa oldArticle = saveArticle("Vecchio", "Prima riga");
        ArticleJpa newArticle = saveArticle("Nuovo", "Seconda riga");
        long orderId = createOrder(oldArticle.getId());
        WorkOrderJpa updatedWorkOrder = saveWorkOrder("Ordine aggiornato");

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(updatedWorkOrder.getId(), "2026-09-06", "STANDARD",
                                "COMPLETED", newArticle.getId(), 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].articleId").value(newArticle.getId()))
                .andExpect(jsonPath("$.details[0].quantity").value(9));

        OrderJpa updated = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(updated.getDetails()).singleElement().satisfies(detail ->
                assertThat(detail.getArticle().getId()).isEqualTo(newArticle.getId()));
    }

    @Test
    void failedUpdateKeepsExistingOrderUnchanged() throws Exception {
        ArticleJpa article = saveArticle("Valido", null);
        long orderId = createOrder(article.getId());
        WorkOrderJpa otherWorkOrder = saveWorkOrder("Non salvare");

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(otherWorkOrder.getId(), "2026-09-08", "LOW",
                                "SHIPPED", 999999L, 7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Uno o più articoli non esistono"));

        OrderJpa unchanged = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Ordine test");
        assertThat(unchanged.getStatus().name()).isEqualTo("PROCESSING");
        assertThat(unchanged.getDetails().getFirst().getArticle().getId()).isEqualTo(article.getId());
    }

    @Test
    void invalidLinesAndDuplicateArticlesAreRejected() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        WorkOrderJpa emptyWorkOrder = saveWorkOrder("Vuoto");
        WorkOrderJpa quantityWorkOrder = saveWorkOrder("Quantità errata");
        WorkOrderJpa duplicateWorkOrder = saveWorkOrder("Duplicato");

        mockMvc.perform(post("/api/orders")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workOrderId":%d,"date":"2026-09-05","priority":"LOW",
                                 "status":"DRAFT","details":[]}
                                """.formatted(emptyWorkOrder.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.details")
                        .value("L'ordine deve contenere almeno una riga"));

        mockMvc.perform(post("/api/orders")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(quantityWorkOrder.getId(), "2026-09-05", "LOW",
                                "DRAFT", article.getId(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['details[0].quantity']")
                        .value("La quantità deve essere maggiore di zero"));

        mockMvc.perform(post("/api/orders")
                        .with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2026-09-05","priority":"LOW",
                                 "status":"DRAFT","details":[
                                   {"articleId":%d,"quantity":1},
                                   {"articleId":%d,"quantity":2}
                                 ]}
                                """).formatted(duplicateWorkOrder.getId(), article.getId(), article.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Lo stesso articolo non può comparire più volte nell'ordine"));
    }

    @Test
    void workOrderIsRequiredAndMustExist() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);

        mockMvc.perform(post("/api/orders").with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-09-05","priority":"LOW","status":"DRAFT",
                                 "details":[{"articleId":%d,"quantity":1}]}
                                """.formatted(article.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.workOrderId").value("La commessa è obbligatoria"));

        mockMvc.perform(post("/api/orders").with(role("USER_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(999999L, "2026-09-05", "LOW", "DRAFT", article.getId(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La commessa non esiste"));
    }

    @Test
    void listSupportsSearchEnumDateFiltersSortingAndPagination() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        createOrder(article.getId());
        createOrder("Zeta spedito", "2026-09-10", "LOW", "SHIPPED", article.getId());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "1")
                        .param("sort", "{\"name\":\"ASC\"}")
                        .param("filter", "{\"status\":{\"operator\":\"EQUAL\",\"values\":[\"PROCESSING\"]}}")
                        .with(role("USER_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Ordine test"));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"date\":{\"operator\":\"GTE\",\"values\":[\"2026-09-09\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Zeta spedito"));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"SPEDITO\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SHIPPED"));
    }

    @Test
    void deleteCascadesDetailsAndMissingOrdersReturnNotFound() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        long orderId = createOrder(article.getId());

        mockMvc.perform(delete("/api/orders/{id}", orderId)
                        .with(role("USER_ROLE")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(orderRepository.findById(orderId)).isEmpty();

        mockMvc.perform(get("/api/orders/{id}", 999999L).with(role("USER_ROLE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ordine non trovato"));
        mockMvc.perform(delete("/api/orders/{id}", 999999L)
                        .with(role("USER_ROLE")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private long createOrder(Long articleId) throws Exception {
        return createOrder("Ordine test", "2026-09-05", "HIGH", "PROCESSING", articleId);
    }

    private long createOrder(String name, String date, String priority,
                             String status, Long articleId) throws Exception {
        WorkOrderJpa workOrder = saveWorkOrder(name);
        String location = mockMvc.perform(post("/api/orders")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), date, priority, status, articleId, 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String orderJson(Long workOrderId, String date, String priority,
                             String status, Long articleId, int quantity) {
        return ("""
                {"workOrderId":%d,"name":"nome ignorato","date":"%s","priority":"%s","status":"%s",
                 "details":[{"articleId":%d,"quantity":%d}]}
                """).formatted(workOrderId, date, priority, status, articleId, quantity);
    }

    private WorkOrderJpa saveWorkOrder(String name) {
        WorkOrderJpa workOrder = new WorkOrderJpa();
        workOrder.setName(name);
        return workOrderRepository.saveAndFlush(workOrder);
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
