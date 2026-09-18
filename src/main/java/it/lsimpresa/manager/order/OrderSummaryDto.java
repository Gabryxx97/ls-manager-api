package it.lsimpresa.manager.order;

import lombok.Data;
import java.time.LocalDate;
import java.time.Instant;

@Data
public class OrderSummaryDto {
    private Long id;
    private Long workOrderId;
    private String name;
    private LocalDate date;
    private OrderPriority priority;
    private OrderStatus status;
    private OrderCategory category;
    private Long assignedWarehouseId;
    private String assignedWarehouseName;
    private Instant takenInChargeAt;
}
