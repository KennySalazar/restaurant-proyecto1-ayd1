package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detalle específico de un cambio de receta mostrando composición anterior, nueva y cambios identificados.
 */
@Schema(description = "Detalle específico de una versión/cambio de receta de platillo")
public record RecipeVersionChangeDetailResponse(
        @Schema(description = "Identificador del platillo", example = "1")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-001")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Identificador de la versión consultada", example = "2")
        Long versionId,

        @Schema(description = "Número de versión consultada", example = "2")
        Integer versionNumber,

        @Schema(description = "Estado de la versión: VIGENTE, HISTORICA, BORRADOR", example = "VIGENTE")
        String status,

        @Schema(description = "Motivo del cambio", example = "Sustitución de insumo")
        String changeReason,

        @Schema(description = "Fecha desde la que entró en vigencia", example = "2026-09-13T20:00:00Z")
        Instant effectiveFrom,

        @Schema(description = "Fecha hasta la que estuvo vigente", example = "null")
        Instant effectiveTo,

        @Schema(description = "Costo total de producción calculado para esta versión", example = "18.5000")
        BigDecimal totalCost,

        @Schema(description = "Número de versión anterior comparada (null si es la primera versión)", example = "1")
        Integer previousVersionNumber,

        @Schema(description = "Costo total de la versión anterior", example = "16.5000")
        BigDecimal previousTotalCost,

        @Schema(description = "Diferencia de costo total (nueva - anterior)", example = "2.0000")
        BigDecimal totalCostDifference,

        @Schema(description = "Composición anterior (insumos de la versión previa)")
        List<RecipeIngredientResponse> previousComposition,

        @Schema(description = "Nueva composición (insumos de la versión seleccionada)")
        List<RecipeIngredientResponse> newComposition,

        @Schema(description = "Detalle de insumos agregados, retirados o modificados")
        List<RecipeIngredientChangeResponse> changes,

        @Schema(description = "Identificador del usuario que registró la versión", example = "1")
        Long createdById,

        @Schema(description = "Fecha de registro de la versión", example = "2026-09-13T20:00:00Z")
        Instant createdAt
) {
}
