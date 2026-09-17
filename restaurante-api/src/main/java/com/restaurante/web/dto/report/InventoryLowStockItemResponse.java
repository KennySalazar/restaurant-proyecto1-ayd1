package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record InventoryLowStockItemResponse(
        Long insumoId,
        String codigo,
        String nombre,
        String categoria,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        String unidadMedida,
        BigDecimal deficit,
        String estado
) {
}
