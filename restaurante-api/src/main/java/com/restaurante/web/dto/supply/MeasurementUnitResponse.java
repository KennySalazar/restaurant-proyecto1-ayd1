package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unidad de medida disponible para insumos")
public record MeasurementUnitResponse(
                @Schema(description = "Identificador de la unidad de medida", example = "2") Short id,

                @Schema(description = "Código de la unidad", example = "KILOGRAMO") String code,

                @Schema(description = "Nombre de la unidad", example = "Kilogramo") String name,

                @Schema(description = "Abreviatura de la unidad", example = "kg") String abbreviation,

                @Schema(description = "Dimensión física (MASA, VOLUMEN, CONTEO)", example = "MASA") String dimension) {
}
