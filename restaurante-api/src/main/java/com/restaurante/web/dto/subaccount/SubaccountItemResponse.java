package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Detalle de un ítem asignado a una sub-cuenta.
 */
@Schema(description = "Ítem o platillo asignado a una subcuenta")
public record SubaccountItemResponse(
        @Schema(description = "Identificador de la asignación", example = "1")
        Long id,

        @Schema(description = "Identificador del detalle de comanda", example = "15")
        Long comandaDetalleId,

        @Schema(description = "Nombre del platillo o ítem", example = "Hamburguesa Clásica")
        String nombrePlatillo,

        @Schema(description = "Cantidad asignada a esta subcuenta", example = "1.000000")
        BigDecimal cantidadAsignada,

        @Schema(description = "Precio unitario efectivo con modificadores", example = "45.00")
        BigDecimal precioUnitario,

        @Schema(description = "Subtotal de este ítem en la subcuenta", example = "45.00")
        BigDecimal subtotal
) {
}
