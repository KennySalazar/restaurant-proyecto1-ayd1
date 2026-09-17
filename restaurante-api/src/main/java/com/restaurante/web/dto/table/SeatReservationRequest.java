package com.restaurante.web.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud para sentar a un cliente con reserva en una mesa.
 */
@Schema(description = "Datos para registrar la llegada y sentar a un cliente con reserva")
public record SeatReservationRequest(
        @Schema(description = "Identificador de la reserva (opcional si se especifica código o si la mesa tiene reserva única)", example = "10")
        Long reservaId,

        @Schema(description = "Código único de la reserva (ej. RES-ABC12345)", example = "RES-A1B2C3D4")
        String codigoReserva,

        @Schema(description = "Nombre del cliente para verificación", example = "Carlos Morales")
        String nombreCliente,

        @Positive(message = "La cantidad de personas debe ser mayor que cero")
        @Schema(description = "Cantidad de personas a sentar (opcional, por defecto personas de la reserva)", example = "4")
        Short cantidadPersonas,

        @Schema(description = "Observaciones o notas adicionales para la cuenta de la mesa", example = "Cliente solicita silla para bebé")
        String notas
) {
}
