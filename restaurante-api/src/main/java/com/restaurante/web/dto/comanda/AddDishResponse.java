package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Confirmación y detalle de platillos agregados exitosamente a una cuenta abierta.
 */
@Schema(description = "Respuesta tras agregar platillos a la cuenta")
public record AddDishResponse(
        @Schema(description = "Mensaje de confirmación", example = "Platillo(s) registrado(s) exitosamente en la cuenta")
        String mensaje,

        @Schema(description = "Identificador de la cuenta", example = "10")
        Long cuentaId,

        @Schema(description = "Número único de la cuenta", example = "CTA-M-01-12345")
        String numeroCuenta,

        @Schema(description = "Identificador de la mesa", example = "1")
        Long mesaId,

        @Schema(description = "Número de la mesa", example = "M-01")
        String numeroMesa,

        @Schema(description = "Identificador de la comanda en la que se registraron", example = "4")
        Long comandaId,

        @Schema(description = "Número de ronda de la orden", example = "1")
        short numeroRonda,

        @Schema(description = "Platillos agregados con su detalle", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DishOrderDetailResponse> platillos,

        @Schema(description = "Total monetario de los platillos agregados en esta operación", example = "150.00")
        BigDecimal subtotalAgregado
) {
}
