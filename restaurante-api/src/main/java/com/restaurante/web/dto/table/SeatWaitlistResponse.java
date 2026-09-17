package com.restaurante.web.dto.table;

import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.model.WaitlistStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Confirmación y detalle tras sentar exitosamente a un cliente de lista de espera en su mesa.
 */
@Schema(description = "Detalle y confirmación tras sentar a un cliente de la lista de espera en la mesa")
public record SeatWaitlistResponse(
        @Schema(description = "Mensaje de confirmación", example = "Cliente de la lista de espera sentado exitosamente. La mesa cambió a estado ocupada.")
        String mensaje,

        @Schema(description = "Identificador de la mesa asignada", example = "3")
        Long mesaId,

        @Schema(description = "Número de la mesa asignada", example = "M-03")
        String numeroMesa,

        @Schema(description = "Estado resultante de la mesa", example = "OCUPADA")
        TableStatus estadoMesa,

        @Schema(description = "Identificador de la entrada en lista de espera", example = "5")
        Long listaEsperaId,

        @Schema(description = "Nombre del cliente", example = "Maria Lopez")
        String nombreCliente,

        @Schema(description = "Teléfono del cliente", example = "+50255551234")
        String telefonoCliente,

        @Schema(description = "Cantidad de personas sentadas en la mesa", example = "4")
        Short cantidadPersonas,

        @Schema(description = "Estado resultante en la lista de espera", example = "SENTADA")
        WaitlistStatus estadoListaEspera,

        @Schema(description = "Identificador de la cuenta abierta para la mesa", example = "26")
        Long cuentaId,

        @Schema(description = "Número de cuenta generado para la mesa", example = "CTA-M-03-12345")
        String numeroCuenta,

        @Schema(description = "Fecha y hora en que se sentó al cliente")
        Instant sentadoEn
) {
}
