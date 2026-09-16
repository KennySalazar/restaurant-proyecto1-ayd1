package com.restaurante.web.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long cantidadVentas,
        BigDecimal montoTotalVendido,
        List<SalesByCategoryResponse> ventasPorCategoria,
        List<SalesByWaiterResponse> ventasPorMesero
) {
}