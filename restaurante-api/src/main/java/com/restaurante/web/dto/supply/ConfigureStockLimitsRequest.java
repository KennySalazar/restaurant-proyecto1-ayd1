package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Solicitud para configurar los límites de stock de un insumo")
public record ConfigureStockLimitsRequest(
        @Schema(description = "Nivel de stock mínimo para alertas y control de existencias", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "Los límites de stock no pueden ser negativos")
        BigDecimal minimumStock,

        @Schema(description = "Nivel de stock máximo de almacenamiento (opcional, null para dejar sin configurar)", example = "100.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "Los límites de stock no pueden ser negativos")
        BigDecimal maximumStock
) {
}
