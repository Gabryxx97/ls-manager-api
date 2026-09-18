package it.lsimpresa.manager.order;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OrderDto {
    private Long id;
    private Long workOrderId;
    private String name;
    private LocalDate date;
    private OrderPriority priority;
    private OrderStatus status;
    private OrderCategory category;
    private String notes;
    private Long assignedWarehouseId;
    private String assignedWarehouseName;
    private Instant takenInChargeAt;
    private List<Detail> details;
    private BigDecimal netTotal;

    @Data
    public static class Detail {
        private Long id;
        private Long articleId;
        private String articleDescription;
        private Integer quantity;
        private OrderDetailStatus status;
        private Integer pickedQuantity;
        private Integer remainingQuantity;
        private String articleSku;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
