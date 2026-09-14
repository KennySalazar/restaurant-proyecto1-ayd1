package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para actualizar la información comercial y operativa de un platillo registrado.
 */
@Schema(description = "Solicitud para actualizar la información de un platillo registrado")
public record UpdateDishRequest(
        @Schema(description = "Código del platillo (opcional)", example = "PLA-0001")
        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        String code,

        @Schema(description = "Nombre comercial del platillo", example = "Hamburguesa Suprema", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del platillo es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String name,

        @Schema(description = "Descripción del platillo", example = "Hamburguesa con doble carne, tocino y queso cheddar")
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description,

        @Schema(description = "Identificador de la categoría asignada al platillo", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La categoría del platillo es obligatoria")
        Long categoryId,

        @Schema(description = "Precio de venta al público fijado", example = "55.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
        BigDecimal salePrice,

        @Schema(description = "URL de la imagen del platillo", example = "https://images.restaurante.com/dishes/hamburguesa-suprema.jpg")
        @Size(max = 500, message = "La URL de la imagen no puede exceder 500 caracteres")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El tiempo estimado de preparación es obligatorio")
        @Min(value = 1, message = "El tiempo estimado de preparación debe ser mayor que cero")
        Short preparationTimeMinutes,

        @Schema(description = "Disponibilidad manual en el menú (opcional)", example = "true")
        Boolean manualAvailable
) {
}
