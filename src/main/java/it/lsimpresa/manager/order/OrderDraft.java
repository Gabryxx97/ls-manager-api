package it.lsimpresa.manager.order;

import java.time.LocalDate;
import java.util.List;

public record OrderDraft(
        Long workOrderId,
        LocalDate date,
        OrderPriority priority,
        OrderStatus status,
        OrderCategory category,
        String notes,
        List<Detail> details) {

    public OrderDraft(Long workOrderId, LocalDate date, OrderPriority priority,
                      OrderStatus status, List<Detail> details) {
        this(workOrderId, date, priority, status, OrderCategory.HYDRAULIC_HVAC, null, details);
    }

    public record Detail(Long articleId, Integer quantity) {
    }
}
