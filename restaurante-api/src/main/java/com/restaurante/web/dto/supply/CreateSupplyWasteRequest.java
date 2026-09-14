package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Solicitud para registrar la baja de existencias de un insumo por concepto de
 * merma.
 */
@Schema(description = "Datos para registrar una merma de inventario (baja por vencimiento, daño, error de manejo, etc.)")
public record CreateSupplyWasteRequest(
                @Schema(description = "Identificador único del insumo a dar de baja (opcional si se especifica en la URL)", example = "1") Long supplyId,

                @NotNull(message = "La cantidad es obligatoria") @DecimalMin(value = "0.0001", inclusive = true, message = "La cantidad debe ser mayor que cero") @Schema(description = "Cantidad del insumo a dar de baja por merma", example = "5.0000", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal quantity,

                @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres") @Schema(description = "Descripción o justificación detallada del motivo de la merma", example = "Insumo vencido por fecha límite de almacenamiento superada", requiredMode = Schema.RequiredMode.REQUIRED) String reason,

                @Schema(description = "Clasificación formal del motivo (VENCIMIENTO, DANO, ERROR_MANEJO, OTRO)", example = "VENCIMIENTO", allowableValues = {
                                "VENCIMIENTO", "DANO", "ERROR_MANEJO", "OTRO"
                }) String reasonType,

                @Size(max = 80, message = "El número o código de lote no puede exceder los 80 caracteres") @Schema(description = "Lote específico del insumo afectado", example = "LOTE-2026-A1") String batchNumber,

                @Size(max = 255, message = "Las observaciones no pueden exceder los 255 caracteres") @Schema(description = "Observaciones o notas complementarias", example = "Descartado tras control de calidad matutino") String notes,

                @Schema(description = "Fecha de la merma (si se omite, se asigna la fecha y hora actual)", example = "2026-09-13") LocalDate date) {
}
