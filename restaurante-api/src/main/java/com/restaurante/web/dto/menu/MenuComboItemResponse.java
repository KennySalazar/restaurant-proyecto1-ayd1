package com.restaurante.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Platillo incluido dentro de un combo del menú operativo.
 */
@Schema(description = "Detalle de un platillo incluido en un combo")
public record MenuComboItemResponse(
        @Schema(description = "Identificador único del platillo", example = "3")
        Long dishId,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Cantidad de porciones incluidas en el combo", example = "2")
        Short quantity
) {
}