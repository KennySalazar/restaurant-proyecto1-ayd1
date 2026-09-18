package com.restaurante.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Platillo del menú operativo con sus modificadores disponibles.
 */
@Schema(description = "Platillo del menú operativo disponible para armar comandas")
public record MenuDishResponse(
        @Schema(description = "Identificador único del platillo", example = "1")
        Long id,

        @Schema(description = "Código del platillo", example = "PLA-0001")
        String code,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Descripción del platillo", example = "Deliciosa hamburguesa con carne de res, lechuga y tomate")
        String description,

        @Schema(description = "Identificador de la categoría", example = "2")
        Long categoryId,

        @Schema(description = "Nombre de la categoría", example = "Plato fuerte")
        String categoryName,

        @Schema(description = "Precio de venta fijado", example = "45.00")
        BigDecimal salePrice,

        @Schema(description = "URL de la imagen del platillo", example = "https://images.restaurante.com/dishes/hamburguesa.jpg")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        Short preparationTimeMinutes,

        @Schema(description = "Disponibilidad operativa actual del platillo para venta", example = "true")
        Boolean available,

        @Schema(description = "Cantidad máxima de porciones disponibles para preparar según inventario actual", example = "12")
        Integer availablePortions,

        @Schema(description = "Código del motivo de falta de disponibilidad (MANUAL, FALTA_INSUMOS, SIN_RECETA, INACTIVO) o null si está disponible", example = "FALTA_INSUMOS")
        String unavailabilityReason,

        @Schema(description = "Modificadores disponibles para el platillo")
        List<MenuModifierResponse> modifiers
) {
}