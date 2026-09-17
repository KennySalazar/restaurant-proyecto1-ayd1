package com.restaurante.web.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryReportResponse(
        LocalDate fechaGeneracion,
        Long totalInsumos,
        Long totalInsumosBajoStock,
        Long totalInsumosSinCosto,
        BigDecimal valorTotalInventario,
        boolean valorTotalCompleto,
        String advertenciaValorIncompleto,
        List<InventorySupplyValuationResponse> valoracionInventario,
        List<InventoryLowStockItemResponse> insumosBajoStock,
        InventoryWasteSectionResponse seccionMermas
) {
}
