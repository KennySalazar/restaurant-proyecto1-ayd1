package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Solicitud para registrar una entrada de inventario de un insumo")
public record CreateSupplyEntryRequest(
                @Schema(description = "Identificador del insumo (opcional si se especifica en la ruta URL)", example = "1") Long supplyId,

                @Schema(description = "Cantidad del insumo recibida", example = "25.0000", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "La cantidad es obligatoria") @DecimalMin(value = "0.0001", inclusive = true, message = "La cantidad debe ser mayor a cero") BigDecimal quantity,

                @Schema(description = "Fecha de recepción o compra (YYYY-MM-DD)", example = "2026-09-13", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "La fecha es obligatoria") @PastOrPresent(message = "La fecha de recepción no puede ser futura") LocalDate date,

                @Schema(description = "Costo unitario de compra", example = "15.5000", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "El costo de compra es obligatorio") @DecimalMin(value = "0.0", inclusive = true, message = "El costo de compra no puede ser negativo") BigDecimal unitCost,

                @Schema(description = "Nombre o razón social del proveedor (opcional)", example = "Distribuidora San José") @Size(max = 150, message = "El nombre del proveedor no puede exceder 150 caracteres") String supplierName,

                @Schema(description = "Número o referencia del comprobante de compra (opcional)", example = "FAC-2026-00123") @Size(max = 100, message = "La referencia de compra no puede exceder 100 caracteres") String purchaseReference,

                @Schema(description = "Número de lote asignado (opcional)", example = "LOT-202609-01") @Size(max = 80, message = "El lote no puede exceder 80 caracteres") String batchNumber,

                @Schema(description = "Fecha de vencimiento del lote recibido (opcional)", example = "2027-03-31") LocalDate expirationDate,

                @Schema(description = "Observaciones adicionales sobre la entrada (opcional)", example = "Mercadería recibida en buen estado") @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres") String notes) {
}
