package it.lsimpresa.manager.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
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
        private Long articleId;

        @NotNull(message = "La quantità è obbligatoria")
        @Positive(message = "La quantità deve essere maggiore di zero")
        private Integer quantity;

        @Size(max = 2000, message = "La descrizione non può superare 2.000 caratteri")
        private String articleDescription;

        @Size(max = 64, message = "Il codice non può superare 64 caratteri")
        private String articleSku;

        @Size(max = 16, message = "L'unità di misura non può superare 16 caratteri")
        private String unitOfMeasure;

        @DecimalMin(value = "0.00", message = "Il prezzo non può essere negativo")
        @Digits(integer = 10, fraction = 2, message = "Il prezzo deve avere al massimo due decimali")
        private java.math.BigDecimal unitPrice;
    }
}
