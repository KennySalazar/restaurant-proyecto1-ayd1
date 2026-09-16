package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record SalesByCategoryResponse(
        Long categoriaId,
        String categoria,
        BigDecimal cantidadVendida,
        BigDecimal montoVendido
) {
}