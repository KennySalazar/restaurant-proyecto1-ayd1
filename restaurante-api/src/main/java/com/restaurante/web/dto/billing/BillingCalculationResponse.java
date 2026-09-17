package com.restaurante.web.dto.billing;

import java.math.BigDecimal;

public record BillingCalculationResponse(
        Long cuentaId,
        Long subcuentaId,
        String numeroCuenta,
        BigDecimal subtotal,
        BigDecimal porcentajeImpuesto,
        BigDecimal montoImpuesto,
        BigDecimal porcentajePropina,
        BigDecimal montoPropina,
        BigDecimal total
) {
}