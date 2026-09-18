package it.lsimpresa.manager.order;

import jakarta.validation.constraints.NotNull;

public record OrderAssigneeRequestDto(
        @NotNull(message = "Il magazziniere è obbligatorio") Long warehouseUserId) {
}
