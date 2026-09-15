package com.restaurante.web.dto.payment;

import java.math.BigDecimal;
import java.util.List;

public record ChargeResponse(
        Long facturaId,
        String numeroDocumento,
        Long cuentaId,
        BigDecimal totalFactura,
        BigDecimal totalPagado,
        BigDecimal montoPendiente,
        String estadoCuenta,
        List<PaymentResponse> pagos
) {
}