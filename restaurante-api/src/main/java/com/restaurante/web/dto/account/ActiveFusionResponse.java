package com.restaurante.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle de una fusión de mesas actualmente vigente (la cuenta destino sigue activa).
 */
@Schema(description = "Fusión de mesas vigente")
public record ActiveFusionResponse(
        @Schema(description = "Identificador de la mesa origen (fusionada, permanece ocupada físicamente)", example = "3")
        Long mesaOrigenId,

        @Schema(description = "Número de la mesa origen", example = "M-03")
        String numeroMesaOrigen,

        @Schema(description = "Identificador de la mesa destino que concentra la cuenta", example = "4")
        Long mesaDestinoId,

        @Schema(description = "Número de la mesa destino", example = "M-04")
        String numeroMesaDestino,

        @Schema(description = "Identificador de la cuenta destino vigente", example = "12")
        Long cuentaDestinoId,

        @Schema(description = "Cantidad total de personas de la cuenta unificada", example = "6")
        int totalPersonas
) {
}
