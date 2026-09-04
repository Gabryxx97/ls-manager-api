package it.lsimpresa.manager.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderRequestDto {
    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 255, message = "Il nome non può superare 255 caratteri")
    private String name;

    @NotNull(message = "La data è obbligatoria")
    private LocalDate date;

    @NotNull(message = "La priorità è obbligatoria")
    private OrderPriority priority;

    @NotNull(message = "Lo stato è obbligatorio")
    private OrderStatus status;

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
