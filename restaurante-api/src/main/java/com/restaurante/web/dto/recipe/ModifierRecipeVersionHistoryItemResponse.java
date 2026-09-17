package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resumen histórico de una versión de receta de modificador con su composición y cambios respecto a la versión anterior.
 */
@Schema(description = "Resumen histórico de una versión de receta de modificador")
public record ModifierRecipeVersionHistoryItemResponse(
        @Schema(description = "Identificador de la versión de receta del modificador", example = "1")
        Long versionId,

        @Schema(description = "Número de versión", example = "1")
        Integer versionNumber,

        @Schema(description = "Estado de la versión: VIGENTE, HISTORICA, BORRADOR", example = "HISTORICA")
        String status,

        @Schema(description = "Motivo del cambio o registro", example = "Definición inicial de receta de modificador")
        String changeReason,

        @Schema(description = "Fecha desde la que estuvo o está vigente", example = "2026-09-10T10:00:00Z")
        Instant effectiveFrom,

        @Schema(description = "Fecha hasta la que estuvo vigente", example = "2026-09-13T20:00:00Z")
        Instant effectiveTo,

        @Schema(description = "Costo de producción adicional calculado para esta versión", example = "4.5000")
        BigDecimal totalCost,

        @Schema(description = "Cantidad de insumos en esta versión", example = "1")
        Integer ingredientCount,

        @Schema(description = "Composición de insumos preservada para esta versión")
        List<ModifierIngredientResponse> ingredients,

        @Schema(description = "Cambios respecto a la versión inmediata anterior (vacío en versión inicial)")
        List<ModifierIngredientChangeResponse> changes,

        @Schema(description = "Identificador del usuario que registró la versión", example = "1")
        Long createdById,

        @Schema(description = "Fecha de registro de la versión", example = "2026-09-10T10:00:00Z")
        Instant createdAt
) {
}
