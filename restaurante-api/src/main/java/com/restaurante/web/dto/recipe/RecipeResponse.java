package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Información completa de una receta definida para un platillo con sus insumos
 * y cálculo de costo.
 */
@Schema(description = "Información consolidada de la receta de un platillo")
public record RecipeResponse(
                @Schema(description = "Identificador de la versión de receta", example = "1") Long id,

                @Schema(description = "Identificador del platillo", example = "1") Long dishId,

                @Schema(description = "Código del platillo", example = "PLA-001") String dishCode,

                @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica") String dishName,

                @Schema(description = "Número de versión de la receta", example = "1") Integer versionNumber,

                @Schema(description = "Estado de la versión de receta (VIGENTE, HISTORICA, BORRADOR)", example = "VIGENTE") String status,

                @Schema(description = "Motivo de definición o cambio", example = "Definición inicial de receta estándar") String changeReason,

                @Schema(description = "Fecha y hora desde la cual está vigente", example = "2026-09-13T23:59:00Z") Instant effectiveFrom,

                @Schema(description = "Costo total calculado de la receta a partir de los insumos", example = "18.5000") BigDecimal totalCost,

                @Schema(description = "Precio de venta fijado para el platillo", example = "45.00") BigDecimal dishSalePrice,

                @Schema(description = "Margen bruto de ganancia estimado (precio - costo)", example = "26.5000") BigDecimal grossMargin,

                @Schema(description = "Porcentaje de margen de ganancia", example = "58.8889") BigDecimal marginPercentage,

                @Schema(description = "Listado de insumos y cantidades que componen la receta") List<RecipeIngredientResponse> ingredients,

                @Schema(description = "Identificador del usuario que definió la receta", example = "1") Long createdById,

                @Schema(description = "Fecha de creación del registro", example = "2026-09-13T23:59:00Z") Instant createdAt) {
}
