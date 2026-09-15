package com.restaurante.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para transferir una cuenta abierta a otra mesa del restaurante.
 */
@Schema(description = "Datos para transferir una cuenta abierta a una mesa destino")
public record TransferAccountRequest(
        @NotNull(message = "El identificador de la mesa destino es requerido")
        @Schema(description = "Identificador de la mesa destino (debe estar en estado libre)", example = "5")
        Long mesaDestinoId,

        @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
        @Schema(description = "Motivo de la transferencia o reubicación del cliente", example = "Cliente solicitó cambio a mesa en terraza")
        String motivo
) {
}
