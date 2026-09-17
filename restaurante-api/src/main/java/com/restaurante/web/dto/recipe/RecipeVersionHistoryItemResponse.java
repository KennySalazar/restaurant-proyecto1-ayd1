package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resumen histórico de una versión de receta de platillo con su composición y cambios respecto a la versión anterior.
 */
@Schema(description = "Resumen histórico de una versión de receta de platillo")
public record RecipeVersionHistoryItemResponse(
        @Schema(description = "Identificador de la versión de receta", example = "1")
        Long versionId,

        @Schema(description = "Número de versión", example = "1")
        Integer versionNumber,

        @Schema(description = "Estado de la versión: VIGENTE, HISTORICA, BORRADOR", example = "HISTORICA")
        String status,

        @Schema(description = "Motivo del cambio o registro", example = "Definición inicial de receta")
        String changeReason,

        @Schema(description = "Fecha desde la que estuvo o está vigente", example = "2026-09-10T10:00:00Z")
        Instant effectiveFrom,

        @Schema(description = "Fecha hasta la que estuvo vigente", example = "2026-09-13T20:00:00Z")
        Instant effectiveTo,

        @Schema(description = "Costo total de producción calculado para esta versión", example = "18.5000")
        BigDecimal totalCost,

        @Schema(description = "Precio de venta del platillo al momento de consulta", example = "45.00")
        BigDecimal salePrice,

        @Schema(description = "Margen bruto estimado", example = "26.5000")
        BigDecimal grossMargin,

        @Schema(description = "Porcentaje de margen de ganancia", example = "58.8889")
        BigDecimal marginPercentage,

        @Schema(description = "Cantidad de insumos que componen la receta", example = "4")
        Integer ingredientCount,

        @Schema(description = "Composición de insumos preservada para esta versión")
        List<RecipeIngredientResponse> ingredients,

        @Schema(description = "Cambios respecto a la versión inmediata anterior (vacío en versión inicial)")
        List<RecipeIngredientChangeResponse> changes,

        @Schema(description = "Identificador del usuario que registró la versión", example = "1")
        Long createdById,

        @Schema(description = "Fecha de registro de la versión", example = "2026-09-10T10:00:00Z")
        Instant createdAt
) {
}
