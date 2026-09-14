package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Detalle de una entrada de inventario registrada")
public record SupplyEntryResponse(
                @Schema(description = "Identificador único de la entrada de inventario", example = "1") Long entryId,

                @Schema(description = "Identificador del detalle de la entrada", example = "1") Long detailId,

                @Schema(description = "Número correlativo de documento interno", example = "ENT-00001") String documentNumber,

                @Schema(description = "Identificador del insumo recibido", example = "1") Long supplyId,

                @Schema(description = "Código único del insumo", example = "INS-0001") String supplyCode,

                @Schema(description = "Nombre del insumo", example = "Carne de Res Molida") String supplyName,

                @Schema(description = "Unidad de medida del insumo", example = "Kilogramo") String measurementUnit,

                @Schema(description = "Abreviatura de la unidad de medida", example = "kg") String unitAbbreviation,

                @Schema(description = "Cantidad ingresada al inventario", example = "25.0000") BigDecimal quantity,

                @Schema(description = "Costo unitario de compra", example = "15.5000") BigDecimal unitCost,

                @Schema(description = "Costo total de la entrada (cantidad * costo unitario)", example = "387.5000") BigDecimal totalCost,

                @Schema(description = "Stock anterior antes de registrar la entrada", example = "10.0000") BigDecimal previousStock,

                @Schema(description = "Stock disponible resultante tras la entrada", example = "35.0000") BigDecimal currentStock,

                @Schema(description = "Costo unitario promedio ponderado resultante del insumo", example = "15.5000") BigDecimal currentUnitCost,

                @Schema(description = "Fecha de recepción o compra", example = "2026-09-13") LocalDate date,

                @Schema(description = "Nombre o razón social del proveedor", example = "Distribuidora San José") String supplierName,

                @Schema(description = "Referencia del comprobante o factura de compra", example = "FAC-2026-00123") String purchaseReference,

                @Schema(description = "Número de lote asignado", example = "LOT-202609-01") String batchNumber,

                @Schema(description = "Fecha de vencimiento del lote", example = "2027-03-31") LocalDate expirationDate,

                @Schema(description = "Observaciones adicionales de la entrada", example = "Mercadería recibida en buen estado") String notes,

                @Schema(description = "Fecha y hora de registro en el sistema") Instant createdAt) {
}
