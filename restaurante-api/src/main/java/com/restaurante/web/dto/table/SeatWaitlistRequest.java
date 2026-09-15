package com.restaurante.web.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud para sentar a un cliente sugerido de la lista de espera en una mesa.
 */
@Schema(description = "Datos para confirmar y sentar a un cliente sugerido de la lista de espera")
public record SeatWaitlistRequest(
        @Schema(description = "Identificador de la entrada en lista de espera (opcional si la mesa ya tiene sugerencia activa)", example = "5")
        Long listaEsperaId,

        @Positive(message = "La cantidad de personas debe ser mayor que cero")
        @Schema(description = "Cantidad de personas a sentar (opcional, por defecto personas registradas en la lista de espera)", example = "4")
        Short cantidadPersonas,

        @Schema(description = "Observaciones o notas adicionales para la cuenta de la mesa", example = "Cliente aceptó mesa en terraza")
        String notas
) {
}
