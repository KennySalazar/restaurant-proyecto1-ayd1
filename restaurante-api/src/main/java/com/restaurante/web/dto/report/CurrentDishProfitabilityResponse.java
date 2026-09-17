package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record CurrentDishProfitabilityResponse(
        Long platilloId,
        String codigo,
        String platillo,
        String categoria,
        BigDecimal precioVenta,
        BigDecimal costoProduccion,
        BigDecimal gananciaUnitaria,
        BigDecimal margenRentabilidad,
        boolean rentabilidadCalculable,
        String motivoNoCalculable
) {
}