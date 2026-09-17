package com.restaurante.web.dto.table;

import com.restaurante.domain.model.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Confirmación y detalle tras sentar exitosamente a un cliente con reserva en su mesa.
 */
@Schema(description = "Detalle y confirmación tras sentar a un cliente con reserva")
public record SeatReservationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Cliente de la reserva sentado exitosamente. La mesa cambió a estado ocupada.")
        String mensaje,

        @Schema(description = "Identificador de la mesa", example = "2")
        Long mesaId,

        @Schema(description = "Número de la mesa asignada", example = "M-02")
        String numeroMesa,

        @Schema(description = "Estado resultante de la mesa", example = "OCUPADA")
        TableStatus estadoMesa,

        @Schema(description = "Identificador de la reserva atendida", example = "10")
        Long reservaId,

        @Schema(description = "Código de la reserva", example = "RES-A1B2C3D4")
        String codigoReserva,

        @Schema(description = "Nombre del cliente de la reserva", example = "Carlos Morales")
        String nombreCliente,

        @Schema(description = "Cantidad de personas sentadas en la mesa", example = "4")
        Short cantidadPersonas,

        @Schema(description = "Identificador de la cuenta abierta para la mesa", example = "25")
        Long cuentaId,

        @Schema(description = "Número de cuenta generado para la mesa", example = "CTA-M-02-12345")
        String numeroCuenta,

        @Schema(description = "Fecha y hora en que se sentó al cliente")
        Instant sentadoEn
) {
}
