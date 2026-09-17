package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para registrar un nuevo platillo en el catálogo del restaurante.
 */
@Schema(description = "Datos requeridos para el registro de un nuevo platillo")
public record CreateDishRequest(
        @NotBlank(message = "El nombre del platillo es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String name,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        @Schema(description = "Descripción detallada del platillo", example = "Deliciosa hamburguesa con carne de res, lechuga y tomate")
        String description,

        @NotNull(message = "La categoría del platillo es obligatoria")
        @Schema(description = "Identificador de la categoría del platillo", example = "2")
        Long categoryId,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener como máximo 10 enteros y 2 decimales")
        @Schema(description = "Precio de venta fijado para el platillo", example = "45.00")
        BigDecimal salePrice,

        @Size(max = 500, message = "La URL de la imagen no puede exceder 500 caracteres")
        @Schema(description = "URL de la imagen del platillo", example = "https://images.restaurante.com/dishes/hamburguesa.jpg")
        String imageUrl,

        @NotNull(message = "El tiempo estimado de preparación es obligatorio")
        @Min(value = 1, message = "El tiempo de preparación debe ser mayor que cero")
        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        Short preparationTimeMinutes,

        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        @Schema(description = "Código único del platillo (opcional, se autogenera si se omite)", example = "PLA-0001")
        String code
) {
}
