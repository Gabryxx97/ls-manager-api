package it.lsimpresa.manager.order;

import java.time.LocalDate;
import java.util.List;

public record OrderDraft(
        Long workOrderId,
        LocalDate date,
        OrderPriority priority,
        OrderCategory category,
        String notes,
        List<Detail> details) {

    public record Detail(Long articleId, Integer quantity) {
    }
}
