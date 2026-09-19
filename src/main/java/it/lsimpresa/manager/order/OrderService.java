package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import it.lsimpresa.manager.exception.ResourceNotAccessExeption;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRole;
import it.lsimpresa.manager.user.UserRepository;
import it.lsimpresa.manager.exception.ConflictException;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private static final Set<OrderStatus> ASSIGNABLE_STATUSES = Set.of(OrderStatus.PROCESSING);
    @Transactional
    public OrderJpa create(OrderDraft draft) {
        OrderJpa order = new OrderJpa();
        apply(draft, order, true);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa create(OrderDraft draft, UserJpa creator) {
        return create(draft, creator, creator != null && creator.getRole() == UserRole.ADMIN_ROLE);
    }

    @Transactional
    public OrderJpa create(OrderDraft draft, UserJpa creator, boolean admin) {
        validateCreationDate(draft, admin);
        OrderJpa order = new OrderJpa();
        order.setCreatedBy(creator);
        apply(draft, order, true);
        return orderRepository.save(order);
    }

    private void validateCreationDate(OrderDraft draft, boolean admin) {
        if (draft == null || draft.date() == null) return;
        LocalDate minimum = LocalDate.now(ZoneId.of("Europe/Rome")).plusDays(admin ? 1 : 2);
        if (draft.date().isBefore(minimum)) {
            throw new IllegalArgumentException(admin
                    ? "La data dell'ordine deve essere almeno domani"
                    : "La data dell'ordine deve essere almeno dopodomani");
        }
    }

    @Transactional
    public OrderJpa update(Long id, OrderDraft draft) {
        OrderJpa order = findDetailedForUpdate(id);
        assertStructurallyEditable(order);
        apply(draft, order, false);
        return orderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        OrderJpa order = findDetailedForUpdate(id);
        assertStructurallyEditable(order);
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

    @Transactional
    public OrderJpa claim(Long id, UserJpa warehouse) {
        if (warehouse.getRole() != UserRole.WAREHOUSE_ROLE) {
            throw new IllegalArgumentException("Solo un magazziniere può prendere in carico un ordine");
        }
        if (orderRepository.claimIfAvailable(id, warehouse, ASSIGNABLE_STATUSES, Instant.now()) == 1) {
            return findDetailed(id);
        }
        OrderJpa order = findEntity(id);
        if (!ASSIGNABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Non è possibile prendere in carico un ordine chiuso");
        }
        throw new ConflictException("L'ordine è già preso in carico da un altro magazziniere");
    }

    @Transactional
    public OrderJpa assign(Long id, Long warehouseUserId) {
        OrderJpa order = findDetailedForUpdate(id);
        assertAssignable(order);
        UserJpa warehouse = userRepository.findById(warehouseUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Magazziniere non trovato"));
        if (warehouse.getRole() != UserRole.WAREHOUSE_ROLE) {
            throw new IllegalArgumentException("L'utente selezionato non è un magazziniere");
        }
        order.setAssignedWarehouse(warehouse);
        markTakenInCharge(order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa revokeAssignment(Long id) {
        OrderJpa order = findDetailedForUpdate(id);
        assertAssignable(order);
        order.setAssignedWarehouse(null);
        return orderRepository.save(order);
    }

    private void assertAssignable(OrderJpa order) {
        if (!ASSIGNABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Non è possibile modificare l'assegnazione di un ordine chiuso");
        }
    }

    @Transactional
    public OrderJpa updatePickedQuantity(Long orderId, Long detailId, Integer pickedQuantity,
                                         UserJpa actor, boolean admin) {
        OrderJpa order = findDetailedForUpdate(orderId);
        assertWorkflowAccess(order, actor, admin);
        OrderDetailJpa detail = findDetail(order, detailId);
        if (detail.getStatus() == OrderDetailStatus.TO_PURCHASE
                || detail.getStatus() == OrderDetailStatus.CANCELED
                || detail.getStatus() == OrderDetailStatus.REPLACED) {
            throw new ConflictException("La riga non può essere aggiornata nello stato corrente");
        }
        validatePickedQuantity(detail, pickedQuantity);
        detail.setPickedQuantity(pickedQuantity);
        detail.setStatus(pickedQuantity.equals(detail.getQuantity())
                ? OrderDetailStatus.COMPLETED : OrderDetailStatus.TO_PICK);
        markTakenInCharge(order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa markToPurchase(Long orderId, Long detailId, Integer pickedQuantity,
                                   UserJpa actor, boolean admin) {
        OrderJpa order = findDetailedForUpdate(orderId);
        assertWorkflowAccess(order, actor, admin);
        OrderDetailJpa detail = findDetail(order, detailId);
        if (detail.getStatus() != OrderDetailStatus.TO_PICK) {
            throw new ConflictException("Solo una riga da prelevare può essere contrassegnata da ordinare");
        }
        validatePickedQuantity(detail, pickedQuantity);
        if (pickedQuantity.equals(detail.getQuantity())) {
            throw new IllegalArgumentException("Non è possibile ordinare una riga già prelevata completamente");
        }
        detail.setPickedQuantity(pickedQuantity);
        detail.setStatus(OrderDetailStatus.TO_PURCHASE);
        markTakenInCharge(order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa resumePicking(Long orderId, Long detailId, UserJpa actor, boolean admin) {
        OrderJpa order = findDetailedForUpdate(orderId);
        assertWorkflowAccess(order, actor, admin);
        OrderDetailJpa detail = findDetail(order, detailId);
        if (detail.getStatus() != OrderDetailStatus.TO_PURCHASE) {
            throw new ConflictException("Solo una riga da ordinare può tornare al prelievo");
        }
        detail.setStatus(OrderDetailStatus.TO_PICK);
        markTakenInCharge(order);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderJpa complete(Long id, UserJpa actor, boolean admin) {
        OrderJpa order = findDetailedForUpdate(id);
        assertWorkflowAccess(order, actor, admin);
        if (order.getDetails().stream().anyMatch(detail -> detail.getStatus() != OrderDetailStatus.COMPLETED)) {
            throw new ConflictException("Tutte le righe devono essere completate prima di completare l'ordine");
        }
        order.setStatus(OrderStatus.COMPLETED);
        markTakenInCharge(order);
        return orderRepository.save(order);
    }

    private void apply(OrderDraft draft, OrderJpa order, boolean creating) {
        if (draft == null) {
            throw new IllegalArgumentException("I dati dell'ordine sono obbligatori");
        }
        if (draft.workOrderId() == null) {
            throw new IllegalArgumentException("La commessa è obbligatoria");
        }
        if (draft.date() == null || draft.priority() == null) {
            throw new IllegalArgumentException("Data e priorità sono obbligatorie");
        }
        if (draft.category() == null) {
            throw new IllegalArgumentException("La categoria è obbligatoria");
        }
        if (draft.details() == null || draft.details().isEmpty()) {
            throw new IllegalArgumentException("L'ordine deve contenere almeno una riga");
        }

        Set<Long> articleIds = validateAndCollectDetails(draft.details());
        WorkOrderJpa workOrder = workOrderRepository.findById(draft.workOrderId())
                .orElseThrow(() -> new IllegalArgumentException("La commessa non esiste"));
        Map<Long, ArticleJpa> articles = new LinkedHashMap<>();
        articleRepository.findAllById(articleIds)
                .forEach(article -> articles.put(article.getId(), article));

        if (articles.size() != articleIds.size()) {
            throw new IllegalArgumentException("Uno o più articoli non esistono");
        }

        List<OrderDetailJpa> details = new ArrayList<>();
        Map<Long, BigDecimal> historicalPrices = new java.util.HashMap<>();
        order.getDetails().forEach(detail -> {
            if (detail.getArticle() != null && detail.getArticle().getId() != null) {
                historicalPrices.put(detail.getArticle().getId(), detail.getUnitPrice());
            }
        });
        draft.details().forEach(draftDetail -> {
            OrderDetailJpa detail = new OrderDetailJpa();
            detail.setQuantity(draftDetail.quantity());
            detail.setPickedQuantity(0);
            detail.setStatus(OrderDetailStatus.TO_PICK);

            if (draftDetail.articleId() != null) {
                ArticleJpa article = articles.get(draftDetail.articleId());
                detail.setArticle(article);
                BigDecimal historical = historicalPrices.get(draftDetail.articleId());
                BigDecimal price = historical != null ? historical : article.getUnitPrice();
                if (price == null) {
                    throw new IllegalArgumentException("Uno o più articoli non hanno un prezzo unitario");
                }
                detail.setUnitPrice(price);
            } else {
                detail.setCustomDescription(normalizeRequired(draftDetail.articleDescription()));
                detail.setCustomSku(normalizeOptional(draftDetail.articleSku()));
                detail.setCustomUnitOfMeasure(normalizeOptional(draftDetail.unitOfMeasure()));
                detail.setUnitPrice(draftDetail.unitPrice());
            }
            details.add(detail);
        });

        order.setWorkOrder(workOrder);
        order.setName(workOrder.getName());
        order.setDate(draft.date());
        order.setPriority(draft.priority());
        if (creating) {
            order.setStatus(OrderStatus.PROCESSING);
        }
        order.setCategory(draft.category());
        order.setNotes(normalizeNotes(draft.notes()));
        order.replaceDetails(details);
    }

    private OrderJpa findDetailedForUpdate(Long id) {
        return orderRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordine non trovato"));
    }

    private OrderDetailJpa findDetail(OrderJpa order, Long detailId) {
        return order.getDetails().stream()
                .filter(detail -> detail.getId().equals(detailId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Riga ordine non trovata"));
    }

    private void assertStructurallyEditable(OrderJpa order) {
        if (order.getTakenInChargeAt() != null || order.getStatus() != OrderStatus.PROCESSING) {
            throw new ConflictException("L'ordine preso in carico non può più essere modificato o eliminato");
        }
    }

    private void assertWorkflowAccess(OrderJpa order, UserJpa actor, boolean admin) {
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new ConflictException("Un ordine chiuso non può essere lavorato");
        }
        if (admin) return;
        if (actor == null || actor.getRole() != UserRole.WAREHOUSE_ROLE
                || order.getAssignedWarehouse() == null
                || !order.getAssignedWarehouse().getId().equals(actor.getId())) {
            throw new ResourceNotAccessExeption("Solo il magazziniere assegnato può lavorare questo ordine");
        }
    }

    private void validatePickedQuantity(OrderDetailJpa detail, Integer pickedQuantity) {
        if (pickedQuantity == null || pickedQuantity < 0) {
            throw new IllegalArgumentException("La quantità prelevata non può essere negativa");
        }
        if (pickedQuantity > detail.getQuantity()) {
            throw new IllegalArgumentException("La quantità prelevata non può superare quella richiesta");
        }
    }

    private void markTakenInCharge(OrderJpa order) {
        if (order.getTakenInChargeAt() == null) {
            order.setTakenInChargeAt(Instant.now());
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) return null;
        return notes.trim();
    }

    private Set<Long> validateAndCollectDetails(List<OrderDraft.Detail> details) {
        Set<Long> articleIds = new HashSet<>();

        for (OrderDraft.Detail detail : details) {
            if (detail == null) {
                throw new IllegalArgumentException("La riga ordine è obbligatoria");
            }
            if (detail.quantity() == null || detail.quantity() <= 0) {
                throw new IllegalArgumentException("La quantità deve essere maggiore di zero");
            }

            if (detail.articleId() != null) {
                if (hasText(detail.articleDescription()) || hasText(detail.articleSku())
                        || hasText(detail.unitOfMeasure()) || detail.unitPrice() != null) {
                    throw new IllegalArgumentException("Una riga censita non può contenere dati liberi");
                }
                if (!articleIds.add(detail.articleId())) {
                    throw new IllegalArgumentException("Lo stesso articolo non può comparire più volte nell'ordine");
                }
            } else {
                if (!hasText(detail.articleDescription()) || detail.unitPrice() == null) {
                    throw new IllegalArgumentException("La riga non censita richiede descrizione e prezzo unitario");
                }
                validateCustomPrice(detail.unitPrice());
            }
        }
        return articleIds;
    }

    private void validateCustomPrice(BigDecimal price) {
        BigDecimal normalized = price.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        int integerDigits = normalized.precision() - normalized.scale();
        if (price.signum() < 0 || scale > 2 || integerDigits > 10) {
            throw new IllegalArgumentException("Il prezzo deve essere non negativo e avere al massimo due decimali");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim().replaceAll("\\s+", " ") : null;
    }
}
