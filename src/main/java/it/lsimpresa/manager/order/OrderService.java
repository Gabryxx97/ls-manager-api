package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.workorder.WorkOrderJpa;
import it.lsimpresa.manager.workorder.WorkOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;
    private final WorkOrderRepository workOrderRepository;
    @Transactional
    public OrderJpa create(OrderDraft draft) {
        OrderJpa order = new OrderJpa();
        apply(draft, order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa create(OrderDraft draft, UserJpa creator) {
        OrderJpa order = new OrderJpa();
        order.setCreatedBy(creator);
        apply(draft, order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa update(Long id, OrderDraft draft) {
        OrderJpa order = findEntity(id);
        apply(draft, order);
        return orderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        OrderJpa order = findEntity(id);
        orderRepository.delete(order);
    }

    public OrderJpa findEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));
    }

    @Transactional
    public OrderJpa findDetailed(Long id) {
        OrderJpa order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));
        return order;
    }

    private void apply(OrderDraft draft, OrderJpa order) {
        if (draft == null) {
            throw new IllegalArgumentException("I dati dell'ordine sono obbligatori");
        }
        if (draft.workOrderId() == null) {
            throw new IllegalArgumentException("La commessa è obbligatoria");
        }
        if (draft.date() == null || draft.priority() == null || draft.status() == null) {
            throw new IllegalArgumentException("Data, priorità e stato sono obbligatori");
        }
        if (draft.details() == null || draft.details().isEmpty()) {
            throw new IllegalArgumentException("L'ordine deve contenere almeno una riga");
        }

        Map<Long, Integer> quantities = validateAndCollectDetails(draft.details());
        WorkOrderJpa workOrder = workOrderRepository.findById(draft.workOrderId())
                .orElseThrow(() -> new IllegalArgumentException("La commessa non esiste"));
        Map<Long, ArticleJpa> articles = new LinkedHashMap<>();
        articleRepository.findAllById(quantities.keySet())
                .forEach(article -> articles.put(article.getId(), article));

        if (articles.size() != quantities.size()) {
            throw new IllegalArgumentException("Uno o più articoli non esistono");
        }

        List<OrderDetailJpa> details = new ArrayList<>();
        Map<Long, BigDecimal> historicalPrices = new java.util.HashMap<>();
        order.getDetails().forEach(detail -> {
            if (detail.getArticle() != null && detail.getArticle().getId() != null) {
                historicalPrices.put(detail.getArticle().getId(), detail.getUnitPrice());
            }
        });
        quantities.forEach((articleId, quantity) -> {
            OrderDetailJpa detail = new OrderDetailJpa();
            detail.setArticle(articles.get(articleId));
            detail.setQuantity(quantity);
            BigDecimal historical = historicalPrices.get(articleId);
            BigDecimal price = historical != null ? historical : articles.get(articleId).getUnitPrice();
            if (price == null) {
                throw new IllegalArgumentException("Uno o più articoli non hanno un prezzo unitario");
            }
            detail.setUnitPrice(price);
            details.add(detail);
        });

        order.setWorkOrder(workOrder);
        order.setName(workOrder.getName());
        order.setDate(draft.date());
        order.setPriority(draft.priority());
        order.setStatus(draft.status());
        order.setCategory(draft.category() == null ? OrderCategory.HYDRAULIC_HVAC : draft.category());
        order.setNotes(normalizeNotes(draft.notes()));
        order.replaceDetails(details);
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) return null;
        return notes.trim();
    }

    private Map<Long, Integer> validateAndCollectDetails(List<OrderDraft.Detail> details) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Set<Long> articleIds = new HashSet<>();

        for (OrderDraft.Detail detail : details) {
            if (detail == null || detail.articleId() == null) {
                throw new IllegalArgumentException("Ogni riga deve riferirsi a un articolo");
            }
            if (detail.quantity() == null || detail.quantity() <= 0) {
                throw new IllegalArgumentException("La quantità deve essere maggiore di zero");
            }
            if (!articleIds.add(detail.articleId())) {
                throw new IllegalArgumentException("Lo stesso articolo non può comparire più volte nell'ordine");
            }
            quantities.put(detail.articleId(), detail.quantity());
        }
        return quantities;
    }
}
