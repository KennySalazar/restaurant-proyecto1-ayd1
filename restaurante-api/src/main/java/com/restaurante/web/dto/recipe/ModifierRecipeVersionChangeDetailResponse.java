package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detalle específico de un cambio de receta de modificador mostrando composición anterior, nueva y cambios identificados.
 */
@Schema(description = "Detalle específico de una versión/cambio de receta de modificador")
public record ModifierRecipeVersionChangeDetailResponse(
        @Schema(description = "Identificador del modificador", example = "1")
        Long modifierId,

        @Schema(description = "Código del modificador", example = "MOD-001")
        String modifierCode,

        @Schema(description = "Nombre del modificador", example = "Extra Queso")
        String modifierName,

        @Schema(description = "Precio adicional fijado para el modificador", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Identificador de la versión consultada", example = "2")
        Long versionId,

        @Schema(description = "Número de versión consultada", example = "2")
        Integer versionNumber,

        @Schema(description = "Estado de la versión: VIGENTE, HISTORICA, BORRADOR", example = "VIGENTE")
        String status,

        @Schema(description = "Motivo del cambio", example = "Aumento de porción")
        String changeReason,

        @Schema(description = "Fecha desde la que entró en vigencia", example = "2026-09-13T20:00:00Z")
        Instant effectiveFrom,

        @Schema(description = "Fecha hasta la que estuvo vigente", example = "null")
        Instant effectiveTo,

        @Schema(description = "Costo de producción de esta versión", example = "4.5000")
        BigDecimal totalCost,

        @Schema(description = "Número de versión anterior comparada (null si es la primera versión)", example = "1")
        Integer previousVersionNumber,

        @Schema(description = "Costo de producción de la versión anterior", example = "3.0000")
        BigDecimal previousTotalCost,

        @Schema(description = "Diferencia de costo (nueva - anterior)", example = "1.5000")
        BigDecimal totalCostDifference,

        @Schema(description = "Composición anterior (insumos de la versión previa)")
        List<ModifierIngredientResponse> previousComposition,

        @Schema(description = "Nueva composición (insumos de la versión seleccionada)")
        List<ModifierIngredientResponse> newComposition,

        @Schema(description = "Detalle de insumos agregados, retirados o modificados")
        List<ModifierIngredientChangeResponse> changes,

        @Schema(description = "Identificador del usuario que registró la versión", example = "1")
        Long createdById,

        @Schema(description = "Fecha de registro de la versión", example = "2026-09-13T20:00:00Z")
        Instant createdAt
) {
}
