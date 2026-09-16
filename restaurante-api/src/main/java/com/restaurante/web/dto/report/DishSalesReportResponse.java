package com.restaurante.web.dto.report;

import java.time.LocalDate;
import java.util.List;

public record DishSalesReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean huboVentas,
        List<DishSalesReportItemResponse> masVendidos,
        List<DishSalesReportItemResponse> menosVendidos
) {
}