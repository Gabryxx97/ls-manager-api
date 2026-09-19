package it.lsimpresa.manager.order;

import it.lsimpresa.manager.utils.search.Searchable;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.workorder.WorkOrderJpa;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "warehouse_order")
public class OrderJpa implements Searchable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderJpa workOrder;

    @NotNull
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private OrderPriority priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private OrderCategory category;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private UserJpa createdBy;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "assigned_warehouse_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private UserJpa assignedWarehouse;

    @Column(name = "taken_in_charge_at")
    private Instant takenInChargeAt;

    @Column(name = "search", nullable = false, columnDefinition = "text")
    private String search;

    @NotEmpty
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Setter(lombok.AccessLevel.NONE)
    private List<@Valid OrderDetailJpa> details = new ArrayList<>();

    public void replaceDetails(List<OrderDetailJpa> newDetails) {
        details.clear();
        if (newDetails != null) {
            newDetails.forEach(this::addDetail);
        }
    }

    public void addDetail(OrderDetailJpa detail) {
        if (detail == null) {
            throw new IllegalArgumentException("La riga ordine è obbligatoria");
        }
        detail.setOrder(this);
        details.add(detail);
    }

    @Override
    @PrePersist
    @PreUpdate
    public void searchFill() {
        validateDetails();
        if (workOrder == null || workOrder.getId() == null) {
            throw new IllegalArgumentException("La commessa è obbligatoria");
        }
        name = workOrder.getName();
        search = name;
    }

    private void validateDetails() {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("L'ordine deve contenere almeno una riga");
        }

        Set<Long> articleIds = new HashSet<>();
        for (OrderDetailJpa detail : details) {
            if (detail.getArticle() == null) {
                if (detail.getCustomDescription() == null || detail.getCustomDescription().isBlank()
                        || detail.getUnitPrice() == null) {
                    throw new IllegalArgumentException("La riga non censita richiede descrizione e prezzo unitario");
                }
            } else {
                if (detail.getArticle().getId() == null) {
                    throw new IllegalArgumentException("L'articolo della riga non è valido");
                }
                if (detail.getCustomDescription() != null || detail.getCustomSku() != null
                        || detail.getCustomUnitOfMeasure() != null) {
                    throw new IllegalArgumentException("Una riga censita non può contenere dati liberi");
                }
                if (!articleIds.add(detail.getArticle().getId())) {
                    throw new IllegalArgumentException("Lo stesso articolo non può comparire più volte nell'ordine");
                }
            }
            if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
                throw new IllegalArgumentException("La quantità deve essere maggiore di zero");
            }
            if (detail.getPickedQuantity() == null || detail.getPickedQuantity() < 0
                    || detail.getPickedQuantity() > detail.getQuantity()) {
                throw new IllegalArgumentException("La quantità prelevata non è valida");
            }
        }
    }
}
