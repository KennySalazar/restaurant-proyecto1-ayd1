package com.restaurante.web.dto.invoice;

import java.math.BigDecimal;

public record InvoicePaymentResponse(
        Long pagoId,
        String metodoCodigo,
        String metodoNombre,
        BigDecimal monto,
        BigDecimal montoRecibido,
        BigDecimal cambioEntregado,
        String referencia,
        String autorizacion
) {
}