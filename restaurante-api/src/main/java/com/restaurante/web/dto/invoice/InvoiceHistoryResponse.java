package com.restaurante.web.dto.invoice;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceHistoryResponse(
        Long facturaId,
        String numeroDocumento,
        Instant emitidaEn,
        Long cuentaId,
        String numeroCuenta,
        Long mesaId,
        String numeroMesa,
        Long meseroId,
        String meseroNombres,
        String meseroApellidos,
        String clienteNombres,
        String clienteApellidos,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal montoImpuesto,
        BigDecimal montoPropina,
        BigDecimal total,
        String estado
) {
}