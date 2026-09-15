package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Confirmación y resumen de la división de cuenta.
 */
@Schema(description = "Resultado de la división de una cuenta")
public record SplitAccountResponse(
        @Schema(description = "Mensaje descriptivo", example = "Cuenta dividida exitosamente")
        String mensaje,

        @Schema(description = "Identificador de la cuenta dividida", example = "10")
        Long cuentaId,

        @Schema(description = "Número de la cuenta principal", example = "CTA-M-01-12345")
        String numeroCuenta,

        @Schema(description = "Tipo de división realizada ('PERSONAS' o 'ITEMS')", example = "PERSONAS")
        String tipoDivision,

        @Schema(description = "Subtotal global de la cuenta principal", example = "360.00")
        BigDecimal subtotalCuenta,

        @Schema(description = "Cantidad total de subcuentas generadas", example = "3")
        int totalSubcuentas,

        @Schema(description = "Detalle de las subcuentas generadas")
        List<SubaccountResponse> subcuentas
) {
}
