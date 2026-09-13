package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoría de insumos")
public record SupplyCategoryResponse(
                @Schema(description = "Identificador de la categoría", example = "1") Long id,

                @Schema(description = "Nombre de la categoría", example = "Proteina") String name,

                @Schema(description = "Descripción de la categoría", example = "Carnes, aves, pescados y otras proteinas") String description) {
}
