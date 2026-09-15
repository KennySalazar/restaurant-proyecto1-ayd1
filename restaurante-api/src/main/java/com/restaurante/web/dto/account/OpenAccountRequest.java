package com.restaurante.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para abrir una cuenta asociada a una mesa libre.
 */
@Schema(description = "Datos para la apertura de una cuenta en una mesa")
public record OpenAccountRequest(
        @Schema(description = "Identificador de la mesa (opcional si se especifica en la ruta URL)", example = "1")
        Long mesaId,

        @Positive(message = "La cantidad de personas debe ser mayor que cero")
        @Schema(description = "Cantidad de personas en la mesa (opcional, por defecto 1)", example = "4")
        Short cantidadPersonas,

        @Schema(description = "Identificador opcional del cliente registrado", example = "10")
        Long clienteId,

        @Size(max = 500, message = "Las observaciones no pueden exceder los 500 caracteres")
        @Schema(description = "Observaciones o notas iniciales para la cuenta", example = "Cliente solicita mesa junto a la ventana")
        String observaciones
) {
}
