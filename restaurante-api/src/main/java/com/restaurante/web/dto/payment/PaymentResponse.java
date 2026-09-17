package com.restaurante.web.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long pagoId,
        Long facturaId,
        Long cuentaId,
        Long subcuentaId,
        String metodoPago,
        BigDecimal monto,
        BigDecimal montoRecibido,
        BigDecimal cambioEntregado,
        String referencia,
        String autorizacion,
        BigDecimal totalFactura,
        BigDecimal totalPagado,
        BigDecimal montoPendiente,
        String estadoCuenta,
        Instant pagadoEn
) {
}