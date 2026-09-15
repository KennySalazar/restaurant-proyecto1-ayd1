package com.restaurante.web.dto.kardex;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representación detallada de un movimiento en el kardex de inventario.
 */
@Schema(description = "Registro de movimiento en el kardex de inventario")
public record KardexRecordResponse(
        @Schema(description = "Identificador único del movimiento", example = "101")
        Long id,

        @Schema(description = "Identificador del insumo", example = "5")
        Long supplyId,

        @Schema(description = "Código del insumo", example = "INS-0005")
        String supplyCode,

        @Schema(description = "Nombre del insumo", example = "Carne de res molida")
        String supplyName,

        @Schema(description = "Unidad de medida", example = "g")
        String measurementUnit,

        @Schema(description = "Código técnico del tipo de movimiento", example = "SALIDA_VENTA")
        String type,

        @Schema(description = "Descripción legible del tipo de movimiento", example = "Salida por venta")
        String typeDescription,

        @Schema(description = "Naturaleza o categoría del movimiento: ENTRADA, SALIDA o AJUSTE", example = "SALIDA")
        String movementNature,

        @Schema(description = "Indica si el movimiento corresponde a un ajuste manual", example = "false")
        boolean isAdjustment,

        @Schema(description = "Efecto sobre las existencias: AUMENTO o DISMINUCION", example = "DISMINUCION")
        String stockEffect,

        @Schema(description = "Cantidad del movimiento", example = "300.0000")
        BigDecimal quantity,

        @Schema(description = "Stock anterior al movimiento", example = "1500.0000")
        BigDecimal previousStock,

        @Schema(description = "Stock resultante tras el movimiento", example = "1200.0000")
        BigDecimal resultingStock,

        @Schema(description = "Costo unitario capturado en el movimiento", example = "0.0500")
        BigDecimal unitCost,

        @Schema(description = "Costo total del movimiento", example = "15.0000")
        BigDecimal totalCost,

        @Schema(description = "Motivo o justificación del movimiento", example = "Consumo automático al enviar comanda 1")
        String reason,

        @Schema(description = "Identificador del usuario responsable", example = "2")
        Long responsibleUserId,

        @Schema(description = "Nombre completo del usuario responsable", example = "Juan Pérez")
        String responsibleUserName,

        @Schema(description = "Código de empleado del usuario responsable", example = "EMP-0002")
        String responsibleUserCode,

        @Schema(description = "Fecha y hora del registro del movimiento")
        Instant createdAt,

        @Schema(description = "Identificador de la comanda de origen (si aplica)", example = "1")
        Long comandaId,

        @Schema(description = "Identificador del detalle de comanda (si aplica)", example = "15")
        Long comandaDetailId,

        @Schema(description = "Número de ronda de la comanda (si aplica)", example = "1")
        Short comandaRound,

        @Schema(description = "Nombre del platillo o combo que originó el consumo (si aplica)", example = "Hamburguesa Clásica")
        String orderItemName,

        @Schema(description = "Identificador del usuario que envió la comanda (si aplica)", example = "4")
        Long comandaSenderId,

        @Schema(description = "Nombre del usuario que envió la comanda (si aplica)", example = "Carlos Mesero")
        String comandaSenderName,

        @Schema(description = "Identificador del detalle de entrada por compra (si aplica)", example = "8")
        Long entryDetailId,

        @Schema(description = "Número de documento de la entrada/compra (si aplica)", example = "ENT-20260914-0001")
        String entryDocumentNumber,

        @Schema(description = "Identificador del detalle de merma (si aplica)", example = "12")
        Long wasteDetailId,

        @Schema(description = "Número de documento de la merma (si aplica)", example = "MER-20260914-0001")
        String wasteDocumentNumber
) {
}
