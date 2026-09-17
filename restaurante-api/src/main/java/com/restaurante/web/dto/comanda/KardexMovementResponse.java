package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detalle de un movimiento generado en el kardex de inventario.
 */
@Schema(description = "Detalle de un movimiento de inventario en kardex")
public record KardexMovementResponse(
        @Schema(description = "Identificador del movimiento", example = "101")
        Long id,

        @Schema(description = "Identificador del insumo", example = "5")
        Long supplyId,

        @Schema(description = "Código del insumo", example = "INS-0005")
        String supplyCode,

        @Schema(description = "Nombre del insumo", example = "Carne de res molida")
        String supplyName,

        @Schema(description = "Unidad de medida de stock", example = "g")
        String unit,

        @Schema(description = "Tipo de movimiento", example = "SALIDA_VENTA")
        String type,

        @Schema(description = "Cantidad descontada", example = "300.0000")
        BigDecimal quantity,

        @Schema(description = "Stock anterior", example = "1500.0000")
        BigDecimal previousStock,

        @Schema(description = "Stock resultante", example = "1200.0000")
        BigDecimal resultingStock,

        @Schema(description = "Motivo del movimiento", example = "Consumo automatico al enviar comanda 1")
        String reason,

        @Schema(description = "Identificador del usuario responsable", example = "2")
        Long responsibleUserId,

        @Schema(description = "Fecha y hora del movimiento")
        Instant createdAt
) {
}
