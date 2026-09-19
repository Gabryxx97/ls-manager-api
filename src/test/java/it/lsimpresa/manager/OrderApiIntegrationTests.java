package it.lsimpresa.manager;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.order.OrderJpa;
import it.lsimpresa.manager.order.OrderRepository;
import it.lsimpresa.manager.workorder.WorkOrderJpa;
import it.lsimpresa.manager.workorder.WorkOrderRepository;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import it.lsimpresa.manager.user.UserRole;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

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

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    @AfterEach
    void clearData() {
        orderRepository.deleteAll();
        workOrderRepository.deleteAll();
        articleRepository.deleteAll();
        if (userRepository.findByUsername("test-user") == null) {
            UserJpa user = new UserJpa();
            user.setUsername("test-user");
            user.setPassword("test-password");
            user.setRole(UserRole.OPERATOR_ROLE);
            user.setAccountActive(true);
            userRepository.saveAndFlush(user);
        }
    }

    @Test
    void bothRolesCanListOrdersWhileAnonymousUserCannot() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("OPERATOR_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("WAREHOUSE_ROLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorCanOnlyAccessOwnOrdersWhileWarehouseCanAccessAllOrders() throws Exception {
        UserJpa other = new UserJpa();
        other.setUsername("other-user");
        other.setPassword("test-password");
        other.setRole(UserRole.OPERATOR_ROLE);
        other.setAccountActive(true);
        userRepository.saveAndFlush(other);
        ArticleJpa article = saveArticle("Articolo", null);
        WorkOrderJpa workOrder = saveWorkOrder("Ordine riservato");

        String location = mockMvc.perform(post("/api/orders")
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), "2099-09-05", "LOW", "DRAFT", article.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        long orderId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(get("/api/orders").param("page", "1").param("pageSize", "10")
                        .with(role("other-user", "OPERATOR_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/0"));
        mockMvc.perform(get("/api/orders/{id}", orderId).with(role("other-user", "OPERATOR_ROLE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/orders/{id}", orderId).with(role("other-user", "OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), "2099-09-06", "HIGH", "PROCESSING", article.getId(), 2)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}", orderId).with(role("WAREHOUSE_ROLE")))
                .andExpect(status().isOk());
    }

    @Test
    void warehouseCanClaimAnAvailableOrderOnlyOnce() throws Exception {
        ArticleJpa article = saveArticle("Articolo presa in carico", null);
        long orderId = createAssignableOrder(article.getId());
        saveWarehouse("warehouse-claim");
        saveWarehouse("warehouse-second");

        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role("warehouse-claim", "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedWarehouseName").value("warehouse-claim"));

        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role("warehouse-second", "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanAssignReassignAndRevokeWarehouse() throws Exception {
        ArticleJpa article = saveArticle("Articolo assegnazione", null);
        long orderId = createAssignableOrder(article.getId());
        UserJpa first = saveWarehouse("warehouse-first");
        UserJpa second = saveWarehouse("warehouse-second-admin");

        mockMvc.perform(put("/api/orders/{id}/assignee", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseUserId\":%d}".formatted(first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedWarehouseId").value(first.getId()));

        mockMvc.perform(put("/api/orders/{id}/assignee", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseUserId\":%d}".formatted(second.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedWarehouseId").value(second.getId()));

        mockMvc.perform(delete("/api/orders/{id}/assignee", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedWarehouseId").doesNotExist());
    }

    @Test
    void assignmentRejectsClosedOrdersAndUnauthorizedRoles() throws Exception {
        ArticleJpa article = saveArticle("Articolo chiuso", null);
        long closedOrder = createOrder("Ordine chiuso", LocalDate.now().plusDays(1).toString(), "LOW", "COMPLETED", article.getId());
        OrderJpa closed = orderRepository.findById(closedOrder).orElseThrow();
        closed.setStatus(it.lsimpresa.manager.order.OrderStatus.COMPLETED);
        orderRepository.saveAndFlush(closed);
        UserJpa warehouse = saveWarehouse("warehouse-closed");

        mockMvc.perform(post("/api/orders/{id}/claim", closedOrder)
                        .with(role("warehouse-closed", "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/orders/{id}/assignee", closedOrder)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseUserId\":%d}".formatted(warehouse.getId())))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/orders/{id}/claim", closedOrder)
                        .with(role("OPERATOR_ROLE")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/orders/{id}/assignee", closedOrder)
                        .with(role("WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseUserId\":%d}".formatted(warehouse.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnsDetailedOrderAndListReturnsOnlyHeader() throws Exception {
        ArticleJpa article = saveArticle("Bullone M8", "Zincato");
        WorkOrderJpa workOrder = saveWorkOrder("  Ordine   urgente  ");

        String response = mockMvc.perform(post("/api/orders")
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), "2099-09-05", "HIGH",
                                "DRAFT", article.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/orders/\\d+")))
                .andExpect(jsonPath("$.name").value("Ordine urgente"))
                .andExpect(jsonPath("$.workOrderId").value(workOrder.getId()))
                .andExpect(jsonPath("$.date").value("2099-09-05"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.details[0].status").value("TO_PICK"))
                .andExpect(jsonPath("$.details[0].pickedQuantity").value(0))
                .andExpect(jsonPath("$.details[0].articleId").value(article.getId()))
                .andExpect(jsonPath("$.details[0].articleDescription").value("Zincato"))
                .andExpect(jsonPath("$.details[0].quantity").value(3))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("\"id\"");
        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .with(role("OPERATOR_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Ordine urgente"))
                .andExpect(jsonPath("$[0].details").doesNotExist());
    }

    @Test
    void createsMixedOrderWithCustomDetailWithoutChangingCatalog() throws Exception {
        ArticleJpa article = saveArticle("Articolo censito", null);
        WorkOrderJpa workOrder = saveWorkOrder("Ordine misto");
        long catalogSize = articleRepository.count();

        String location = mockMvc.perform(post("/api/orders")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"STANDARD","category":"HARDWARE_MISC",
                                 "details":[
                                   {"articleId":%d,"quantity":2},
                                   {"articleDescription":"Guarnizione speciale","articleSku":"G-SP","unitOfMeasure":"PZ","unitPrice":2.50,"quantity":4}
                                 ]}
                                """).formatted(workOrder.getId(), article.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[1].custom").value(true))
                .andExpect(jsonPath("$.details[1].articleId").doesNotExist())
                .andExpect(jsonPath("$.details[1].articleSku").value("G-SP"))
                .andExpect(jsonPath("$.details[1].articleDescription").value("Guarnizione speciale"))
                .andExpect(jsonPath("$.details[1].unitOfMeasure").value("PZ"))
                .andExpect(jsonPath("$.details[1].unitPrice").value(2.50))
                .andExpect(jsonPath("$.details[1].status").value("TO_PICK"))
                .andExpect(jsonPath("$.netTotal").value(12.00))
                .andReturn().getResponse().getHeader("Location");

        assertThat(articleRepository.count()).isEqualTo(catalogSize);
        long orderId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        OrderJpa saved = orderRepository.findDetailedById(orderId).orElseThrow();
        assertThat(saved.getDetails().get(1).getArticle()).isNull();

        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "full").param("format", "csv")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("G-SP")))
                .andExpect(content().string(containsString("Guarnizione speciale")));

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-06","priority":"HIGH","category":"HARDWARE_MISC",
                                 "details":[{"articleDescription":"Guarnizione modificata","unitPrice":3.75,"quantity":2}]}
                                """).formatted(workOrder.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].custom").value(true))
                .andExpect(jsonPath("$.details[0].articleSku").value("NON CENSITO"))
                .andExpect(jsonPath("$.details[0].articleDescription").value("Guarnizione modificata"))
                .andExpect(jsonPath("$.netTotal").value(7.50));
        assertThat(articleRepository.count()).isEqualTo(catalogSize);
    }

    @Test
    void rejectsIncompleteAndMixedCustomDetails() throws Exception {
        ArticleJpa article = saveArticle("Articolo protetto", null);
        WorkOrderJpa workOrder = saveWorkOrder("Righe non valide");

        mockMvc.perform(post("/api/orders").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"LOW","category":"HYDRAULIC",
                                 "details":[{"articleDescription":"Senza prezzo","quantity":1}]}
                                """).formatted(workOrder.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La riga non censita richiede descrizione e prezzo unitario"));

        mockMvc.perform(post("/api/orders").with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"LOW","category":"HYDRAULIC",
                                 "details":[{"articleId":%d,"articleDescription":"Dato manomesso","unitPrice":1.00,"quantity":1}]}
                                """).formatted(workOrder.getId(), article.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Una riga censita non può contenere dati liberi"));
    }

    @Test
    void creationDateRequiresTwoDaysForNonAdminAndAllowsTomorrowForAdmin() throws Exception {
        ArticleJpa article = saveArticle("Articolo data", null);
        WorkOrderJpa operatorWorkOrder = saveWorkOrder("Ordine operatore");
        WorkOrderJpa adminWorkOrder = saveWorkOrder("Ordine admin");
        String tomorrow = LocalDate.now(java.time.ZoneId.of("Europe/Rome")).plusDays(1).toString();

        mockMvc.perform(post("/api/orders")
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(operatorWorkOrder.getId(), tomorrow, "LOW", "DRAFT", article.getId(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La data dell'ordine deve essere almeno dopodomani"));

        mockMvc.perform(post("/api/orders")
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(adminWorkOrder.getId(), tomorrow, "LOW", "DRAFT", article.getId(), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(tomorrow));
    }

    @Test
    void getReturnsHeaderAndLines() throws Exception {
        ArticleJpa article = saveArticle("Dado M8", null);
        long orderId = createOrder(article.getId());

        mockMvc.perform(get("/api/orders/{id}", orderId).with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].articleDescription").value("Dado M8"));
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
                        .content(orderJson(updatedWorkOrder.getId(), "2099-09-06", "STANDARD",
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
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(otherWorkOrder.getId(), "2099-09-08", "LOW",
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
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"LOW",
                                 "status":"DRAFT","details":[]}
                                """.formatted(emptyWorkOrder.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.details")
                        .value("L'ordine deve contenere almeno una riga"));

        mockMvc.perform(post("/api/orders")
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(quantityWorkOrder.getId(), "2099-09-05", "LOW",
                                "DRAFT", article.getId(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['details[0].quantity']")
                        .value("La quantità deve essere maggiore di zero"));

        mockMvc.perform(post("/api/orders")
                        .with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"LOW",
                                 "status":"DRAFT","category":"HYDRAULIC","details":[
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

        mockMvc.perform(post("/api/orders").with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2099-09-05","priority":"LOW","status":"DRAFT",
                                 "details":[{"articleId":%d,"quantity":1}]}
                                """.formatted(article.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.workOrderId").value("La commessa è obbligatoria"));

        mockMvc.perform(post("/api/orders").with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(999999L, "2099-09-05", "LOW", "DRAFT", article.getId(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La commessa non esiste"));
    }

    @Test
    void categoryIsRequired() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        WorkOrderJpa workOrder = saveWorkOrder("Senza categoria");

        mockMvc.perform(post("/api/orders").with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"workOrderId":%d,"date":"2099-09-05","priority":"LOW",
                                 "status":"DRAFT","details":[{"articleId":%d,"quantity":1}]}
                                """).formatted(workOrder.getId(), article.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").value("La categoria è obbligatoria"));
    }

    @Test
    void articleWithoutPriceCannotBeAddedToAnOrder() throws Exception {
        ArticleJpa article = saveArticle("Senza prezzo", "Da valorizzare");
        article.setUnitPrice(null);
        articleRepository.saveAndFlush(article);
        WorkOrderJpa workOrder = saveWorkOrder("Ordine senza prezzo");

        mockMvc.perform(post("/api/orders").with(role("OPERATOR_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(workOrder.getId(), "2099-09-05", "LOW",
                                "DRAFT", article.getId(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Uno o più articoli non hanno un prezzo unitario"));
    }

    @Test
    void listSupportsSearchEnumDateFiltersSortingAndPagination() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        createOrder(article.getId());
        long completedId = createOrder("Zeta completato", "2099-09-10", "LOW", "COMPLETED", article.getId());
        OrderJpa completed = orderRepository.findById(completedId).orElseThrow();
        completed.setStatus(it.lsimpresa.manager.order.OrderStatus.COMPLETED);
        orderRepository.saveAndFlush(completed);

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "1")
                        .param("sort", "{\"name\":\"ASC\"}")
                        .param("filter", "{\"status\":{\"operator\":\"EQUAL\",\"values\":[\"PROCESSING\"]}}")
                        .with(role("OPERATOR_ROLE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "items 0-0/1"))
                .andExpect(jsonPath("$[0].name").value("Ordine test"));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"date\":{\"operator\":\"GTE\",\"values\":[\"2099-09-09\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Zeta completato"));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1").param("pageSize", "10")
                        .param("filter", "{\"search\":{\"operator\":\"ILIKE\",\"values\":[\"COMPLETATO\"]}}")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void deleteCascadesDetailsAndMissingOrdersReturnNotFound() throws Exception {
        ArticleJpa article = saveArticle("Articolo", null);
        long orderId = createOrder(article.getId());

        mockMvc.perform(delete("/api/orders/{id}", orderId)
                        .with(role("OPERATOR_ROLE")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(orderRepository.findById(orderId)).isEmpty();

        mockMvc.perform(get("/api/orders/{id}", 999999L).with(role("OPERATOR_ROLE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ordine non trovato"));
        mockMvc.perform(delete("/api/orders/{id}", 999999L)
                        .with(role("OPERATOR_ROLE")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignedWarehouseCanWorkPartialQuantityPurchaseResumeAndComplete() throws Exception {
        ArticleJpa article = saveArticle("Articolo workflow", null);
        article.setStockQuantity(new java.math.BigDecimal("25.00"));
        articleRepository.saveAndFlush(article);
        long orderId = createAssignableOrder(article.getId());
        UserJpa warehouse = saveWarehouse("warehouse-workflow");

        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.takenInChargeAt").exists());

        Long detailId = orderRepository.findDetailedById(orderId).orElseThrow()
                .getDetails().getFirst().getId();

        mockMvc.perform(put("/api/orders/{orderId}/details/{detailId}/picked-quantity", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("TO_PICK"))
                .andExpect(jsonPath("$.details[0].remainingQuantity").value(1));

        mockMvc.perform(post("/api/orders/{orderId}/details/{detailId}/to-purchase", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("TO_PURCHASE"));

        mockMvc.perform(post("/api/orders/{id}/complete", orderId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/orders/{orderId}/details/{detailId}/resume-picking", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("TO_PICK"));

        mockMvc.perform(put("/api/orders/{orderId}/details/{detailId}/picked-quantity", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.details[0].remainingQuantity").value(0));

        mockMvc.perform(put("/api/orders/{orderId}/details/{detailId}/picked-quantity", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("TO_PICK"));

        mockMvc.perform(put("/api/orders/{orderId}/details/{detailId}/picked-quantity", orderId, detailId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details[0].status").value("COMPLETED"));

        mockMvc.perform(post("/api/orders/{id}/complete", orderId)
                        .with(role(warehouse.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(articleRepository.findById(article.getId()).orElseThrow().getStockQuantity())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void workflowRejectsOtherWarehouseAndLocksStructureAfterRevocation() throws Exception {
        ArticleJpa article = saveArticle("Articolo protetto", null);
        long orderId = createAssignableOrder(article.getId());
        UserJpa assigned = saveWarehouse("warehouse-assigned");
        UserJpa other = saveWarehouse("warehouse-other");

        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role(assigned.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk());
        Long detailId = orderRepository.findDetailedById(orderId).orElseThrow()
                .getDetails().getFirst().getId();

        mockMvc.perform(put("/api/orders/{orderId}/details/{detailId}/picked-quantity", orderId, detailId)
                        .with(role(other.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedQuantity\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/orders/{id}/assignee", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.takenInChargeAt").exists());

        WorkOrderJpa replacement = saveWorkOrder("Non modificabile");
        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(replacement.getId(), "2099-10-01", "LOW", "PROCESSING", article.getId(), 1)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/orders/{id}", orderId)
                        .with(role("ADMIN_ROLE")).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role(other.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedWarehouseId").value(other.getId()));
    }

    @Test
    void adminCanExportFullOrderAsCsvAndPdf() throws Exception {
        ArticleJpa article = saveArticle("Bullone export", "Bullone zincato");
        long orderId = createOrder(article.getId());

        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "full").param("format", "csv")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("ordine-" + orderId + "-completo.csv")))
                .andExpect(content().string(containsString("Quantità richiesta")))
                .andExpect(content().string(containsString("Bullone zincato")));

        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "full").param("format", "pdf")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("ordine-" + orderId + "-completo.pdf")));
    }

    @Test
    void assignedWarehouseCanExportOnlyPurchaseLinesAndOthersCannot() throws Exception {
        ArticleJpa article = saveArticle("Rondella export", "Rondella M8");
        long orderId = createAssignableOrder(article.getId());
        UserJpa assigned = saveWarehouse("warehouse-export-assigned");
        UserJpa other = saveWarehouse("warehouse-export-other");
        mockMvc.perform(post("/api/orders/{id}/claim", orderId)
                        .with(role(assigned.getUsername(), "WAREHOUSE_ROLE")).with(csrf()))
                .andExpect(status().isOk());
        Long detailId = orderRepository.findDetailedById(orderId).orElseThrow().getDetails().getFirst().getId();
        mockMvc.perform(post("/api/orders/{orderId}/details/{detailId}/to-purchase", orderId, detailId)
                        .with(role(assigned.getUsername(), "WAREHOUSE_ROLE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pickedQuantity\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "purchase").param("format", "csv")
                        .with(role(assigned.getUsername(), "WAREHOUSE_ROLE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quantità da ordinare")))
                .andExpect(content().string(containsString(";\"1\";\"€ 1,00\";\"€ 1,00\"")));

        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "purchase").param("format", "csv")
                        .with(role(other.getUsername(), "WAREHOUSE_ROLE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "purchase").param("format", "csv")
                        .with(role("OPERATOR_ROLE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void purchaseExportRejectsOrdersWithoutPurchaseLines() throws Exception {
        ArticleJpa article = saveArticle("Nessun acquisto", null);
        long orderId = createOrder(article.getId());
        mockMvc.perform(get("/api/orders/{id}/export", orderId)
                        .param("scope", "purchase").param("format", "csv")
                        .with(role("ADMIN_ROLE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Non ci sono righe da ordinare per questo ordine"));
    }

    private long createOrder(Long articleId) throws Exception {
        return createOrder("Ordine test", "2099-09-05", "HIGH", "PROCESSING", articleId);
    }

    private long createAssignableOrder(Long articleId) throws Exception {
        return createOrder("Ordine assegnabile", LocalDate.now().plusDays(1).toString(),
                "HIGH", "PROCESSING", articleId);
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
                {"workOrderId":%d,"name":"nome ignorato","date":"%s","priority":"%s","status":"%s","category":"HYDRAULIC",
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
        article.setSku(name.toUpperCase().replaceAll("\\s+", "-"));
        article.setDescription(description == null ? name : description);
        article.setUnitPrice(new java.math.BigDecimal("1.00"));
        return articleRepository.saveAndFlush(article);
    }

    private UserJpa saveWarehouse(String username) {
        UserJpa warehouse = new UserJpa();
        warehouse.setUsername(username);
        warehouse.setPassword("test-password");
        warehouse.setRole(UserRole.WAREHOUSE_ROLE);
        warehouse.setAccountActive(true);
        return userRepository.saveAndFlush(warehouse);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor role(String role) {
        return role("test-user", role);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor role(String username, String role) {
        return user(username).authorities(new SimpleGrantedAuthority(role));
    }
}
