package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public OrderJpa create(OrderDraft draft) {
        OrderJpa order = new OrderJpa();
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
        orderRepository.delete(findEntity(id));
    }

    public OrderJpa findEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));
    }

    @Transactional
    public OrderJpa findDetailed(Long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));
    }

    private void apply(OrderDraft draft, OrderJpa order) {
        if (draft == null) {
            throw new IllegalArgumentException("I dati dell'ordine sono obbligatori");
        }
        if (draft.name() == null || draft.name().isBlank()) {
            throw new IllegalArgumentException("Il nome è obbligatorio");
        }
        if (draft.date() == null || draft.priority() == null || draft.status() == null) {
            throw new IllegalArgumentException("Data, priorità e stato sono obbligatori");
        }
        if (draft.details() == null || draft.details().isEmpty()) {
            throw new IllegalArgumentException("L'ordine deve contenere almeno una riga");
        }

        Map<Long, Integer> quantities = validateAndCollectDetails(draft.details());
        Map<Long, ArticleJpa> articles = new LinkedHashMap<>();
        articleRepository.findAllById(quantities.keySet())
                .forEach(article -> articles.put(article.getId(), article));

        if (articles.size() != quantities.size()) {
            throw new IllegalArgumentException("Uno o più articoli non esistono");
        }

        List<OrderDetailJpa> details = new ArrayList<>();
        quantities.forEach((articleId, quantity) -> {
            OrderDetailJpa detail = new OrderDetailJpa();
            detail.setArticle(articles.get(articleId));
            detail.setQuantity(quantity);
            details.add(detail);
        });

        order.setName(draft.name().trim().replaceAll("\\s+", " "));
        order.setDate(draft.date());
        order.setPriority(draft.priority());
        order.setStatus(draft.status());
        order.replaceDetails(details);
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
