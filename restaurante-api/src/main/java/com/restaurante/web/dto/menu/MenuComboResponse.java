package com.restaurante.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Combo disponible dentro del menú operativo.
 */
@Schema(description = "Combo o promoción disponible para armar comandas")
public record MenuComboResponse(
        @Schema(description = "Identificador único del combo", example = "1")
        Long id,

        @Schema(description = "Código del combo", example = "COM-0001")
        String code,

        @Schema(description = "Nombre comercial del combo", example = "Combo Pareja")
        String name,

        @Schema(description = "Descripción del combo", example = "Incluye 2 hamburguesas clásicas y 2 bebidas")
        String description,

        @Schema(description = "Precio especial de venta fijado para el combo", example = "75.00")
        BigDecimal salePrice,

        @Schema(description = "URL de la imagen del combo", example = "https://images.restaurante.com/combos/combo-pareja.jpg")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "20")
        Short preparationTimeMinutes,

        @Schema(description = "Disponibilidad actual del combo en el menú", example = "true")
        Boolean available,

        @Schema(description = "Listado de platillos incluidos en el combo")
        List<MenuComboItemResponse> items
) {
}