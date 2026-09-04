package it.lsimpresa.manager.order;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OrderSummaryDto {
    private Long id;
    private String name;
    private LocalDate date;
    private OrderPriority priority;
    private OrderStatus status;
}
