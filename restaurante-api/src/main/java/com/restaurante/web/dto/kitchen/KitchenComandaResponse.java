package com.restaurante.web.dto.kitchen;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Representación de una comanda activa para visualización del personal de cocina.
 */
@Schema(description = "Comanda entrante o activa para visualización en cocina")
public record KitchenComandaResponse(
        @Schema(description = "Identificador único de la comanda", example = "105")
        Long id,

        @Schema(description = "Identificador de la cuenta asociada", example = "20")
        Long accountId,

        @Schema(description = "Número de cuenta", example = "CTA-M-12-001")
        String accountNumber,

        @Schema(description = "Identificador de la mesa", example = "12")
        Long tableId,

        @Schema(description = "Número o nombre de la mesa", example = "M-12")
        String tableNumber,

        @Schema(description = "Número de ronda de la comanda", example = "1")
        short roundNumber,

        @Schema(description = "Identificador del mesero responsable", example = "5")
        Long waiterId,

        @Schema(description = "Nombre del mesero responsable", example = "Carlos López")
        String waiterName,

        @Schema(description = "Estado de la comanda en cocina", example = "RECIBIDA")
        String status,

        @Schema(description = "Fecha y hora en que la comanda fue enviada a cocina")
        Instant sentAt,

        @Schema(description = "Fecha y hora de creación de la comanda")
        Instant createdAt,

        @Schema(description = "Minutos transcurridos desde que se envió a cocina (antigüedad)", example = "8")
        long elapsedMinutes,

        @Schema(description = "Tiempo estimado total de preparación en minutos", example = "20")
        short estimatedPreparationTimeMinutes,

        @Schema(description = "Notas generales para cocina", example = "Mesa VIP, enviar todo junto")
        String generalNotes,

        @Schema(description = "Listado de platillos y combos de la comanda")
        List<KitchenComandaItemResponse> items
) {
}
