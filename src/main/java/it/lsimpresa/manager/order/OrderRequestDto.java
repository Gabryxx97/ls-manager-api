package it.lsimpresa.manager.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderRequestDto {
    @NotNull(message = "La commessa è obbligatoria")
    private Long workOrderId;

    @NotNull(message = "La data è obbligatoria")
    private LocalDate date;

    @NotNull(message = "La priorità è obbligatoria")
    private OrderPriority priority;

    @NotNull(message = "La categoria è obbligatoria")
    private OrderCategory category;

    @jakarta.validation.constraints.Size(max = 2000, message = "Le note non possono superare 2.000 caratteri")
    private String notes;

    @NotEmpty(message = "L'ordine deve contenere almeno una riga")
    private List<@Valid Detail> details;

    @Data
    public static class Detail {
        @NotNull(message = "L'articolo è obbligatorio")
        private Long articleId;

        @NotNull(message = "La quantità è obbligatoria")
        @Positive(message = "La quantità deve essere maggiore di zero")
        private Integer quantity;
    }
}
