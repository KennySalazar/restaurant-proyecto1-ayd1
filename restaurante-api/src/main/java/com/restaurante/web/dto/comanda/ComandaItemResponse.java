package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle comercial y operativo de un platillo o combo dentro de una comanda.
 */
@Schema(description = "Detalle de un platillo o combo en una comanda")
public record ComandaItemResponse(
        @Schema(description = "Identificador del detalle", example = "10")
        Long id,

        @Schema(description = "Identificador del platillo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (si aplica)", example = "2")
        Long comboId,

        @Schema(description = "Nombre registrado del platillo o combo", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Precio unitario", example = "45.00")
        BigDecimal unitPrice,

        @Schema(description = "Estado actual del ítem", example = "RECIBIDO")
        String status,

        @Schema(description = "Notas especiales de preparación", example = "Sin cebolla")
        String specialNotes,

        @Schema(description = "Modificadores seleccionados")
        List<String> modifiers
) {
}
