package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Solicitud de asignación de un ítem de comanda a una sub-cuenta.
 */
@Schema(description = "Asignación de un ítem de comanda")
public record AssignItemRequest(
        @NotNull(message = "El identificador del detalle de comanda es requerido")
        @Schema(description = "Identificador del detalle de comanda (platillo/combo)", example = "15")
        Long comandaDetalleId,

        @Schema(description = "Cantidad del platillo a asignar (opcional, si se omite asigna la cantidad completa)", example = "1")
        BigDecimal cantidad
) {
}
