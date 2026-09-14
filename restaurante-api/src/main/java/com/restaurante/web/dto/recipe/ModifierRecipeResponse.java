package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Información completa de la receta de un modificador.
 */
@Schema(description = "Información consolidada de la receta de un modificador")
public record ModifierRecipeResponse(
        @Schema(description = "Identificador de la versión de receta", example = "1")
        Long id,

        @Schema(description = "Identificador del modificador", example = "1")
        Long modifierId,

        @Schema(description = "Código del modificador", example = "MOD-001")
        String modifierCode,

        @Schema(description = "Nombre del modificador", example = "Extra Queso")
        String modifierName,

        @Schema(description = "Precio adicional fijado para el modificador", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Número de versión de la receta", example = "1")
        Integer versionNumber,

        @Schema(description = "Estado de la versión de receta (VIGENTE, HISTORICA, BORRADOR)", example = "VIGENTE")
        String status,

        @Schema(description = "Motivo de cambio o definición", example = "Definición inicial")
        String changeReason,

        @Schema(description = "Fecha desde la cual está vigente", example = "2026-09-13T23:59:00Z")
        Instant effectiveFrom,

        @Schema(description = "Costo total calculado de los insumos del modificador", example = "4.5000")
        BigDecimal totalCost,

        @Schema(description = "Listado de insumos y cantidades que componen la receta")
        List<ModifierIngredientResponse> ingredients,

        @Schema(description = "Identificador del usuario que registró la receta", example = "1")
        Long createdById,

        @Schema(description = "Fecha de creación del registro", example = "2026-09-13T23:59:00Z")
        Instant createdAt
) {
}
