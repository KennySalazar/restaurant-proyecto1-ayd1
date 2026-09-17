package com.restaurante.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Modificador disponible para un platillo dentro del menú operativo.
 */
@Schema(description = "Modificador disponible para selección al armar una comanda")
public record MenuModifierResponse(
        @Schema(description = "Identificador único del modificador", example = "1")
        Long id,

        @Schema(description = "Código del modificador", example = "MOD-0001")
        String code,

        @Schema(description = "Nombre comercial del modificador", example = "Queso extra")
        String name,

        @Schema(description = "Costo o precio adicional al platillo", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Indica si el modificador es obligatorio", example = "false")
        boolean required,

        @Schema(description = "Cantidad máxima de selecciones permitida", example = "1")
        short maxSelections
) {
}