package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record DishSalesReportItemResponse(
        Long platilloId,
        String codigo,
        String nombre,
        Long categoriaId,
        String categoria,
        BigDecimal cantidadVendida
) {
}