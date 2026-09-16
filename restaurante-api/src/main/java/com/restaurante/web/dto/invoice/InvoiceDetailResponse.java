package com.restaurante.web.dto.invoice;

import java.math.BigDecimal;

public record InvoiceDetailResponse(
        Long detalleId,
        String nombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal modificadores,
        BigDecimal subtotalLinea
) {
}