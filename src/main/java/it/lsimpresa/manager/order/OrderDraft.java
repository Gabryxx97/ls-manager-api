package it.lsimpresa.manager.order;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record OrderDraft(
        Long workOrderId,
        LocalDate date,
        OrderPriority priority,
        OrderCategory category,
        String notes,
        List<Detail> details) {

    public record Detail(
            Long articleId,
            Integer quantity,
            String articleDescription,
            String articleSku,
            String unitOfMeasure,
            BigDecimal unitPrice) {

        public Detail(Long articleId, Integer quantity) {
            this(articleId, quantity, null, null, null, null);
        }
    }
}
