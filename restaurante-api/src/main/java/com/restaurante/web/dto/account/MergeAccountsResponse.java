package com.restaurante.web.dto.account;

import com.restaurante.domain.model.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Confirmación y detalle tras fusionar exitosamente dos cuentas de mesas.
 */
@Schema(description = "Detalle y confirmación de la fusión de cuentas")
public record MergeAccountsResponse(
        @Schema(description = "Mensaje de confirmación", example = "Cuentas fusionadas exitosamente en una sola cuenta")
        String mensaje,

        @Schema(description = "Identificador del registro de auditoría de la fusión", example = "1")
        Long fusionId,

        @Schema(description = "Identificador de la cuenta origen que fue fusionada", example = "10")
        Long cuentaOrigenId,

        @Schema(description = "Número de la cuenta origen", example = "CTA-M-03-00010")
        String numeroCuentaOrigen,

        @Schema(description = "Estado resultante de la cuenta origen", example = "FUSIONADA")
        String estadoCuentaOrigen,

        @Schema(description = "Identificador de la mesa origen liberada", example = "3")
        Long mesaOrigenId,

        @Schema(description = "Número de la mesa origen liberada", example = "M-03")
        String numeroMesaOrigen,

        @Schema(description = "Estado resultante de la mesa origen tras la fusión", example = "LIBRE")
        TableStatus estadoMesaOrigen,

        @Schema(description = "Identificador de la cuenta destino que concentra el consumo", example = "12")
        Long cuentaDestinoId,

        @Schema(description = "Número de la cuenta destino", example = "CTA-M-04-00012")
        String numeroCuentaDestino,

        @Schema(description = "Estado resultante de la cuenta destino", example = "ABIERTA")
        String estadoCuentaDestino,

        @Schema(description = "Identificador de la mesa destino que permanece ocupada", example = "4")
        Long mesaDestinoId,

        @Schema(description = "Número de la mesa destino", example = "M-04")
        String numeroMesaDestino,

        @Schema(description = "Estado resultante de la mesa destino", example = "OCUPADA")
        TableStatus estadoMesaDestino,

        @Schema(description = "Cantidad de comandas trasladadas desde la cuenta origen", example = "2")
        int comandasTransferidas,

        @Schema(description = "Total de comandas combinadas en la cuenta destino", example = "5")
        int totalComandasDestino,

        @Schema(description = "Cantidad total de personas sumadas en la cuenta unificada", example = "6")
        int totalPersonas,

        @Schema(description = "Identificador del mesero o usuario que realizó la fusión", example = "2")
        Long realizadoPorId,

        @Schema(description = "Motivo de la fusión", example = "Mesas unidas físicamente")
        String motivo,

        @Schema(description = "Fecha y hora en que se ejecutó la fusión")
        Instant fusionadaEn
) {
}
