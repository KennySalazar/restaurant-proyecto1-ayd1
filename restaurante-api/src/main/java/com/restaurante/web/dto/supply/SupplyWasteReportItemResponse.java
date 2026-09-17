package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Elemento consolidado para el reporte de mermas del inventario.
 */
@Schema(description = "Elemento del reporte de mermas de inventario")
public record SupplyWasteReportItemResponse(
        @Schema(description = "Origen de la merma (DOCUMENTO_MERMA, CANCELACION_COCINA)", example = "DOCUMENTO_MERMA")
        String source,

        @Schema(description = "Identificador de la merma", example = "1")
        Long wasteId,

        @Schema(description = "Número correlativo de documento", example = "MER-00001")
        String documentNumber,

        @Schema(description = "Tipo de motivo (VENCIMIENTO, DANO, ERROR_MANEJO, OTRO)", example = "VENCIMIENTO")
        String reasonType,

        @Schema(description = "Descripción detallada del motivo", example = "Fecha de caducidad superada")
        String generalReason,

        @Schema(description = "Fecha y hora del registro", example = "2026-09-13T23:55:00Z")
        Instant registeredAt,

        @Schema(description = "Identificador del insumo afectado", example = "1")
        Long supplyId,

        @Schema(description = "Nombre del insumo afectado", example = "Tomate Manzano")
        String supplyName,

        @Schema(description = "Categoría del insumo", example = "Verduras")
        String categoryName,

        @Schema(description = "Cantidad dada de baja", example = "3.5000")
        BigDecimal quantity,

        @Schema(description = "Unidad de medida", example = "kg")
        String measurementUnit,

        @Schema(description = "Costo unitario registrado", example = "12.5000")
        BigDecimal unitCost,

        @Schema(description = "Costo total de la pérdida", example = "43.7500")
        BigDecimal totalCost,

        @Schema(description = "Identificador del usuario que registró la merma", example = "1")
        Long registeredById
) {
}
