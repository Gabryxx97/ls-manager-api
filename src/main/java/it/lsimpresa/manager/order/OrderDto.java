package it.lsimpresa.manager.order;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Long workOrderId;
    private String name;
    private LocalDate date;
    private OrderPriority priority;
    private OrderStatus status;
    private List<Detail> details;

    @Data
    public static class Detail {
        private Long id;
        private Long articleId;
        private String articleName;
        private String articleDescription;
        private Integer quantity;
    }
}
