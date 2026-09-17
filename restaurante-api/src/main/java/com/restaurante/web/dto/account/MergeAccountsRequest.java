package com.restaurante.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para fusionar dos cuentas abiertas de mesas del restaurante.
 */
@Schema(description = "Datos para fusionar dos cuentas de mesas abiertas")
public record MergeAccountsRequest(
        @Schema(description = "Identificador de la cuenta origen a fusionar y cerrar", example = "10")
        Long cuentaOrigenId,

        @Schema(description = "Identificador de la cuenta destino que concentrará el consumo", example = "12")
        Long cuentaDestinoId,

        @Schema(description = "Identificador de la mesa origen (si se identifica por mesa)", example = "3")
        Long mesaOrigenId,

        @Schema(description = "Identificador de la mesa destino (si se identifica por mesa)", example = "4")
        Long mesaDestinoId,

        @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
        @Schema(description = "Motivo de la unión de mesas y fusión de cuentas", example = "Mesas unidas físicamente por grupo de clientes")
        String motivo
) {
}
