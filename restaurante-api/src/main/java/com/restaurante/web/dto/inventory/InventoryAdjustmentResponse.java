package com.restaurante.web.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta de confirmación tras registrar un ajuste manual de inventario.
 */
@Schema(description = "Detalle del ajuste manual de inventario registrado")
public record InventoryAdjustmentResponse(
        @Schema(description = "Mensaje de confirmación de la operación", example = "Ajuste manual de inventario registrado correctamente")
        String message,

        @Schema(description = "Identificador del movimiento registrado en el kardex", example = "105")
        Long adjustmentId,

        @Schema(description = "Identificador del insumo ajustado", example = "5")
        Long supplyId,

        @Schema(description = "Código del insumo ajustado", example = "INS-0005")
        String supplyCode,

        @Schema(description = "Nombre del insumo ajustado", example = "Carne de res molida")
        String supplyName,

        @Schema(description = "Sentido del ajuste: AUMENTO o DISMINUCION", example = "AUMENTO")
        String adjustmentType,

        @Schema(description = "Tipo técnico registrado en kardex: AJUSTE_ENTRADA o AJUSTE_SALIDA", example = "AJUSTE_ENTRADA")
        String type,

        @Schema(description = "Cantidad ajustada", example = "25.0000")
        BigDecimal quantity,

        @Schema(description = "Stock anterior al ajuste", example = "100.0000")
        BigDecimal previousStock,

        @Schema(description = "Stock resultante tras el ajuste", example = "125.0000")
        BigDecimal resultingStock,

        @Schema(description = "Unidad de medida del insumo", example = "g")
        String unit,

        @Schema(description = "Motivo del ajuste", example = "Corrección por conteo físico")
        String reason,

        @Schema(description = "Identificador del usuario responsable", example = "1")
        Long responsibleUserId,

        @Schema(description = "Nombre completo del usuario responsable", example = "Admin Restaurante")
        String responsibleUserName,

        @Schema(description = "Fecha y hora del registro del ajuste")
        Instant createdAt,

        @Schema(description = "Indica si el ajuste generó una alerta de inventario bajo", example = "false")
        boolean lowStockAlertGenerated,

        @Schema(description = "Listado de platillos cuya disponibilidad fue evaluada tras el ajuste")
        List<AffectedDishResponse> affectedDishes
) {
}
