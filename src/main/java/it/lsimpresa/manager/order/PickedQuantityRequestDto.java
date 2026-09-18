package it.lsimpresa.manager.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class PickedQuantityRequestDto {
    @NotNull(message = "La quantità prelevata è obbligatoria")
    @PositiveOrZero(message = "La quantità prelevata non può essere negativa")
    private Integer pickedQuantity;
}
