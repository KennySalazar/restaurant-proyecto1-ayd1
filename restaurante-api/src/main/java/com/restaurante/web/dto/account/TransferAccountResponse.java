package com.restaurante.web.dto.account;

import com.restaurante.domain.model.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Confirmación y detalle tras transferir exitosamente una cuenta a otra mesa.
 */
@Schema(description = "Detalle y confirmación de la transferencia de cuenta")
public record TransferAccountResponse(
        @Schema(description = "Mensaje de confirmación", example = "Cuenta transferida exitosamente a la mesa destino")
        String mensaje,

        @Schema(description = "Identificador de la cuenta transferida", example = "15")
        Long cuentaId,

        @Schema(description = "Número único de la cuenta", example = "CTA-M-02-12345")
        String numeroCuenta,

        @Schema(description = "Identificador de la mesa origen", example = "2")
        Long mesaOrigenId,

        @Schema(description = "Número de la mesa origen", example = "M-02")
        String numeroMesaOrigen,

        @Schema(description = "Estado resultante de la mesa origen tras la transferencia", example = "LIBRE")
        TableStatus estadoMesaOrigen,

        @Schema(description = "Identificador de la mesa destino", example = "5")
        Long mesaDestinoId,

        @Schema(description = "Número de la mesa destino", example = "M-05")
        String numeroMesaDestino,

        @Schema(description = "Estado resultante de la mesa destino tras la transferencia", example = "OCUPADA")
        TableStatus estadoMesaDestino,

        @Schema(description = "Cantidad de comandas ya registradas conservadas en la cuenta", example = "3")
        int cantidadComandas,

        @Schema(description = "Identificador del mesero o usuario que realizó la transferencia", example = "8")
        Long transferidoPorId,

        @Schema(description = "Motivo de la transferencia", example = "Cliente solicitó cambio a mesa en terraza")
        String motivo,

        @Schema(description = "Fecha y hora en que se realizó la transferencia")
        Instant transferidaEn
) {
}
