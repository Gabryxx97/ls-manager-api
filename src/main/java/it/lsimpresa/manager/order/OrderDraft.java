package it.lsimpresa.manager.order;

import java.time.LocalDate;
import java.util.List;

public record OrderDraft(
        String name,
        LocalDate date,
        OrderPriority priority,
        OrderStatus status,
        List<Detail> details) {

    public record Detail(Long articleId, Integer quantity) {
    }
}
