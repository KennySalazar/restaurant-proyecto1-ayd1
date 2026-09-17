package com.restaurante.web.dto.payment;

import java.math.BigDecimal;
import java.util.List;

public record ChargeResponse(
        Long facturaId,
        String numeroDocumento,
        Long cuentaId,
        BigDecimal subtotal,
        Long puntosRedimidos,
        BigDecimal descuentoPuntos,
        BigDecimal porcentajeImpuesto,
        BigDecimal montoImpuesto,
        BigDecimal porcentajePropina,
        BigDecimal montoPropina,
        BigDecimal totalFactura,
        Long puntosOtorgados,
        Long saldoPuntosResultante,
        BigDecimal totalPagado,
        BigDecimal montoPendiente,
        String estadoCuenta,
        List<PaymentResponse> pagos


) {
}