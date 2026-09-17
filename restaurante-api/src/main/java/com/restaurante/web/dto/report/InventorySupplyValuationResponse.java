package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record InventorySupplyValuationResponse(
        Long insumoId,
        String codigo,
        String nombre,
        String categoria,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        String unidadMedida,
        BigDecimal costoUnitario,
        BigDecimal valorTotal,
        boolean valorCalculable,
        String advertenciaCosto
) {
}
