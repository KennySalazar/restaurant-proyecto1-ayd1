package com.restaurante.web.dto.account;

import com.restaurante.domain.model.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Confirmación y detalle tras marcar una cuenta como lista para cobro.
 */
@Schema(description = "Detalle y confirmación de la solicitud de cobro de cuenta")
public record RequestBillResponse(
        @Schema(description = "Mensaje de confirmación", example = "Cuenta marcada exitosamente como lista para cobro")
        String mensaje,

        @Schema(description = "Identificador de la cuenta", example = "10")
        Long cuentaId,

        @Schema(description = "Número único de la cuenta", example = "CTA-M-01-12345")
        String numeroCuenta,

        @Schema(description = "Estado resultante de la cuenta", example = "LISTA_COBRO")
        String estadoCuenta,

        @Schema(description = "Fecha y hora en que se solicitó el cobro")
        Instant solicitadaCobroEn,

        @Schema(description = "Identificador de la mesa", example = "1")
        Long mesaId,

        @Schema(description = "Número de la mesa", example = "M-01")
        String numeroMesa,

        @Schema(description = "Estado resultante de la mesa tras la solicitud", example = "CUENTA_SOLICITADA")
        TableStatus estadoMesa,

        @Schema(description = "Cantidad de platillos registrados en la cuenta", example = "3")
        long totalPlatillos,

        @Schema(description = "Indica si se notificó al cajero", example = "true")
        boolean notificadoCajero,

        @Schema(description = "Identificador de la notificación generada para caja", example = "5")
        Long notificacionId
) {
}
