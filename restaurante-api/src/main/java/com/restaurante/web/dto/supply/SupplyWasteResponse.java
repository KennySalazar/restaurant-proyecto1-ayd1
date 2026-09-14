package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detalle completo de una merma de inventario registrada.
 */
@Schema(description = "Información detallada de una merma de inventario registrada")
public record SupplyWasteResponse(
                @Schema(description = "Identificador de la cabecera de la merma", example = "1") Long wasteId,

                @Schema(description = "Identificador del detalle de la merma", example = "1") Long detailId,

                @Schema(description = "Número correlativo de documento asignado a la merma", example = "MER-00001") String documentNumber,

                @Schema(description = "Identificador del insumo dado de baja", example = "1") Long supplyId,

                @Schema(description = "Código del insumo", example = "INS-0001") String supplyCode,

                @Schema(description = "Nombre del insumo", example = "Tomate Manzano") String supplyName,

                @Schema(description = "Nombre de la unidad de medida", example = "Kilogramo") String measurementUnitName,

                @Schema(description = "Abreviatura de la unidad de medida", example = "kg") String measurementUnitAbbreviation,

                @Schema(description = "Cantidad dada de baja por merma", example = "3.5000") BigDecimal quantity,

                @Schema(description = "Costo unitario registrado para la merma", example = "12.5000") BigDecimal unitCost,

                @Schema(description = "Costo total de la pérdida por merma", example = "43.7500") BigDecimal totalCost,

                @Schema(description = "Cantidad en existencias antes de la merma", example = "20.0000") BigDecimal previousStock,

                @Schema(description = "Cantidad en existencias resultante tras la merma", example = "16.5000") BigDecimal resultingStock,

                @Schema(description = "Tipo clasificado de motivo (VENCIMIENTO, DANO, ERROR_MANEJO, OTRO)", example = "VENCIMIENTO") String reasonType,

                @Schema(description = "Motivo detallado de la merma", example = "Fecha de caducidad superada") String reason,

                @Schema(description = "Lote afectado", example = "LOTE-2026-A1") String batchNumber,

                @Schema(description = "Observaciones o notas adicionales", example = "Descartado tras revisión") String notes,

                @Schema(description = "Fecha y hora en que fue registrada la merma", example = "2026-09-13T23:55:00Z") Instant registeredAt) {
}
