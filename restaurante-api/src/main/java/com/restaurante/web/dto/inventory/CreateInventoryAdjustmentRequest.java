package com.restaurante.web.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Solicitud para registrar un ajuste manual de inventario.
 */
@Schema(description = "Solicitud para registrar un ajuste manual de existencias en un insumo")
public record CreateInventoryAdjustmentRequest(
        @Schema(description = "Identificador del insumo a ajustar (opcional si se especifica en la ruta URL)", example = "5")
        Long supplyId,

        @NotBlank(message = "El tipo de ajuste es obligatorio")
        @Schema(description = "Tipo o sentido del ajuste: AUMENTO (o AJUSTE_ENTRADA) o DISMINUCION (o AJUSTE_SALIDA)", example = "AUMENTO")
        String adjustmentType,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
        @Schema(description = "Cantidad a ajustar", example = "50.0000")
        BigDecimal quantity,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Schema(description = "Motivo o justificación del ajuste manual", example = "Corrección por conteo físico mensual")
        String reason
) {
}
