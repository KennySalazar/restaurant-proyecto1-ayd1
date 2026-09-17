package com.restaurante.web.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de la disponibilidad de un platillo afectado tras un ajuste de inventario.
 */
@Schema(description = "Información de disponibilidad de un platillo evaluado tras el ajuste")
public record AffectedDishResponse(
        @Schema(description = "Identificador del platillo", example = "10")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-0010")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Disponibilidad calculada tras el ajuste", example = "true")
        boolean available,

        @Schema(description = "Porciones disponibles estimadas a partir de la receta", example = "15")
        int availablePortions,

        @Schema(description = "Motivo de indisponibilidad si no está disponible", example = "FALTA_INSUMOS")
        String unavailabilityReason
) {
}
