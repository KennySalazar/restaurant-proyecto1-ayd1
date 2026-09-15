package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Representación consolidada de una comanda u orden del restaurante.
 */
@Schema(description = "Detalle general de una comanda")
public record ComandaResponse(
        @Schema(description = "Identificador único de la comanda", example = "1")
        Long id,

        @Schema(description = "Identificador de la cuenta", example = "1")
        Long accountId,

        @Schema(description = "Identificador de la mesa", example = "1")
        Long tableId,

        @Schema(description = "Número de ronda de la comanda en la mesa", example = "1")
        short roundNumber,

        @Schema(description = "Identificador del mesero responsable", example = "2")
        Long waiterId,

        @Schema(description = "Estado actual de la comanda", example = "RECIBIDA")
        String status,

        @Schema(description = "Notas generales de la orden")
        String generalNotes,

        @Schema(description = "Fecha y hora de creación de la comanda")
        Instant createdAt,

        @Schema(description = "Fecha y hora de envío a cocina")
        Instant sentAt,

        @Schema(description = "Listado de platillos y combos incluidos")
        List<ComandaItemResponse> items
) {
}
