package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Información de un platillo incluido dentro de un combo o promoción.
 */
@Schema(description = "Detalle de un platillo incluido en el combo")
public record ComboDetailResponse(
        @Schema(description = "Identificador único del detalle", example = "1")
        Long id,

        @Schema(description = "Identificador único del platillo", example = "3")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-0003")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Precio regular de venta individual del platillo", example = "45.00")
        BigDecimal dishRegularPrice,

        @Schema(description = "Cantidad de porciones incluidas en el combo", example = "2")
        Short quantity,

        @Schema(description = "Orden visual de presentación", example = "0")
        Short visualOrder
) {
}
