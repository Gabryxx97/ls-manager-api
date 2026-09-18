package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.workorder.WorkOrderJpa;
import it.lsimpresa.manager.workorder.WorkOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderModelIntegrationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsEnumValuesAndCascadesOrderDetails() {
        ArticleJpa article = articleRepository.save(article("Bullone"));
        WorkOrderJpa workOrder = workOrderRepository.save(workOrder("Ordine test"));
        OrderJpa order = orderService.create(draft(workOrder.getId(), article.getId(), 4));
        entityManager.flush();
        entityManager.clear();

        Object[] row = (Object[]) entityManager.createNativeQuery(
                        "select priority, status from warehouse_order where id = :id")
                .setParameter("id", order.getId())
                .getSingleResult();
        assertThat(row).containsExactly("HIGH", "PROCESSING");

        OrderJpa reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getDetails()).hasSize(1);
        assertThat(reloaded.getDetails().getFirst().getQuantity()).isEqualTo(4);

        orderService.delete(order.getId());
        entityManager.flush();
        Number remainingDetails = (Number) entityManager.createNativeQuery(
                        "select count(*) from order_detail where order_id = :id")
                .setParameter("id", order.getId())
                .getSingleResult();
        assertThat(remainingDetails.longValue()).isZero();
    }

    @Test
    void updateReplacesOldDetailsThroughOrphanRemoval() {
        ArticleJpa first = articleRepository.save(article("Bullone"));
        ArticleJpa second = articleRepository.save(article("Dado"));
        WorkOrderJpa firstWorkOrder = workOrderRepository.save(workOrder("Ordine test"));
        WorkOrderJpa secondWorkOrder = workOrderRepository.save(workOrder("Ordine aggiornato"));
        OrderJpa order = orderService.create(draft(firstWorkOrder.getId(), first.getId(), 2));
        entityManager.flush();

        OrderDraft update = new OrderDraft(
                secondWorkOrder.getId(),
                LocalDate.of(2026, 9, 6),
                OrderPriority.STANDARD,
                OrderCategory.ELECTRICAL,
                null,
                List.of(new OrderDraft.Detail(second.getId(), 8)));
        orderService.update(order.getId(), update);
        entityManager.flush();
        entityManager.clear();

        OrderJpa reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getDetails()).singleElement().satisfies(detail -> {
            assertThat(detail.getArticle().getId()).isEqualTo(second.getId());
            assertThat(detail.getQuantity()).isEqualTo(8);
        });
    }

    private OrderDraft draft(Long workOrderId, Long articleId, int quantity) {
        return new OrderDraft(
                workOrderId,
                LocalDate.of(2026, 9, 5),
                OrderPriority.HIGH,
                OrderCategory.HYDRAULIC,
                null,
                List.of(new OrderDraft.Detail(articleId, quantity)));
    }

    private WorkOrderJpa workOrder(String name) {
        WorkOrderJpa workOrder = new WorkOrderJpa();
        workOrder.setName(name);
        return workOrder;
    }

    private ArticleJpa article(String name) {
        ArticleJpa article = new ArticleJpa();
        article.setSku(name.toUpperCase());
        article.setDescription("Descrizione");
        article.setUnitPrice(new java.math.BigDecimal("1.00"));
        return article;
    }
}
