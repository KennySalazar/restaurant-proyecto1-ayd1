package com.restaurante.web.dto.kitchen;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Detalle de un platillo o combo en la vista operativa de cocina.
 */
@Schema(description = "Detalle de un platillo u orden en la vista de cocina")
public record KitchenComandaItemResponse(
        @Schema(description = "Identificador del detalle de comanda", example = "10")
        Long id,

        @Schema(description = "Identificador del platillo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (si aplica)", example = "2")
        Long comboId,

        @Schema(description = "Nombre del platillo o combo", example = "Hamburguesa Especial")
        String name,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Estado actual del platillo", example = "RECIBIDO")
        String status,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        short estimatedTimeMinutes,

        @Schema(description = "Notas especiales de preparación", example = "Término medio, sin cebolla")
        String specialNotes,

        @Schema(description = "Modificadores seleccionados", example = "[\"Extra Queso\", \"Tocino\"]")
        List<String> modifiers
) {
}
