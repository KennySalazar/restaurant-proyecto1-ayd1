package com.restaurante.web.dto.account;

import com.restaurante.web.dto.comanda.ComandaItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Detalle operativo de una ronda de comanda en una cuenta de mesa.
 */
@Schema(description = "Detalle operativo de una ronda de comanda en la cuenta")
public record AccountRoundResponse(
        @Schema(description = "Número de ronda dentro de la cuenta", example = "2")
        short roundNumber,

        @Schema(description = "Identificador de la comanda asociada a la ronda", example = "15")
        Long comandaId,

        @Schema(description = "Identificador de la cuenta", example = "3")
        Long accountId,

        @Schema(description = "Identificador de la mesa", example = "2")
        Long tableId,

        @Schema(description = "Número de la mesa", example = "MESA-02")
        String tableNumber,

        @Schema(description = "Estado actual del ciclo de la ronda", example = "RECIBIDA")
        String status,

        @Schema(description = "Notas generales de la ronda", example = "Ronda de bebidas adicionales")
        String generalNotes,

        @Schema(description = "Identificador del mesero asignado", example = "4")
        Long waiterId,

        @Schema(description = "Nombre del mesero asignado", example = "Juan Pérez")
        String waiterName,

        @Schema(description = "Fecha y hora de creación de la ronda")
        Instant createdAt,

        @Schema(description = "Fecha y hora en que la ronda fue enviada a cocina")
        Instant sentAt,

        @Schema(description = "Fecha y hora en que la ronda fue finalizada o entregada")
        Instant finishedAt,

        @Schema(description = "Listado de platillos y combos de esta ronda")
        List<ComandaItemResponse> items,

        @Schema(description = "Cantidad total de rondas registradas en la cuenta", example = "2")
        int totalRoundsInAccount,

        @Schema(description = "Mensaje informativo de la operación")
        String message
) {
}
