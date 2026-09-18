package com.restaurante.application.report.export;

import com.restaurante.application.report.*;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.*;
import com.restaurante.web.dto.report.export.ReportExportFormat;
import com.restaurante.web.dto.report.export.ReportType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportExportService {

    private final SalesReportService salesReportService;
    private final DishSalesReportService dishSalesReportService;
    private final ProfitabilityReportService profitabilityReportService;
    private final TableOccupancyReportService tableOccupancyReportService;
    private final WaiterPerformanceReportService waiterPerformanceReportService;
    private final LoyaltyReportService loyaltyReportService;
    private final InventoryReportService inventoryReportService;

    private final ReportPdfExportService pdfExportService;
    private final ReportExcelExportService excelExportService;

    public ReportExportService(
            SalesReportService salesReportService,
            DishSalesReportService dishSalesReportService,
            ProfitabilityReportService profitabilityReportService,
            TableOccupancyReportService tableOccupancyReportService,
            WaiterPerformanceReportService waiterPerformanceReportService,
            LoyaltyReportService loyaltyReportService,
            InventoryReportService inventoryReportService,
            ReportPdfExportService pdfExportService,
            ReportExcelExportService excelExportService) {

        this.salesReportService = salesReportService;
        this.dishSalesReportService = dishSalesReportService;
        this.profitabilityReportService = profitabilityReportService;
        this.tableOccupancyReportService = tableOccupancyReportService;
        this.waiterPerformanceReportService = waiterPerformanceReportService;
        this.loyaltyReportService = loyaltyReportService;
        this.inventoryReportService = inventoryReportService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    public ReportExportResult export(
            ReportType type,
            ReportExportFormat format,
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        ReportExportData data =
                buildReport(
                        type,
                        startDate,
                        endDate,
                        authentication
                );

        byte[] content;

        String extension;
        String contentType;

        if (format == ReportExportFormat.PDF) {

            content =
                    pdfExportService.generate(data);

            extension = "pdf";
            contentType = "application/pdf";

        } else {

            content =
                    excelExportService.generate(data);

            extension = "xlsx";

            contentType =
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        String fileName =
                buildFileName(
                        type,
                        startDate,
                        endDate,
                        extension
                );

        return new ReportExportResult(
                fileName,
                contentType,
                content
        );
    }

    private ReportExportData buildReport(
            ReportType type,
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        if (type != ReportType.RENTABILIDAD_ACTUAL && type != ReportType.INVENTARIO) {
            requirePeriod(startDate, endDate);
        }

        return switch (type) {

            case VENTAS ->
                    buildSales(
                            startDate,
                            endDate,
                            authentication
                    );

            case PLATILLOS_VENDIDOS ->
                    buildDishSales(
                            startDate,
                            endDate,
                            authentication
                    );

            case RENTABILIDAD_ACTUAL ->
                    buildCurrentProfitability(
                            authentication
                    );

            case RENTABILIDAD_HISTORICA ->
                    buildHistoricalProfitability(
                            startDate,
                            endDate,
                            authentication
                    );

            case OCUPACION_MESAS ->
                    buildTableOccupancy(
                            startDate,
                            endDate,
                            authentication
                    );

            case DESEMPENO_MESEROS ->
                    buildWaiterPerformance(
                            startDate,
                            endDate,
                            authentication
                    );

            case FIDELIZACION ->
                    buildLoyalty(
                            startDate,
                            endDate,
                            authentication
                    );

            case INVENTARIO ->
                    buildInventory(
                            startDate,
                            endDate,
                            authentication
                    );
        };
    }

    private ReportExportData buildSales(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        SalesReportResponse report =
                salesReportService.getReport(
                        startDate,
                        endDate,
                        authentication
                );

        List<List<String>> rows =
                new ArrayList<>();

        for (SalesByCategoryResponse category :
                report.ventasPorCategoria()) {

            rows.add(List.of(
                    "Categoria",
                    text(category.categoriaId()),
                    text(category.categoria()),
                    text(category.cantidadVendida()),
                    text(category.montoVendido())
            ));
        }

        for (SalesByWaiterResponse waiter :
                report.ventasPorMesero()) {

            rows.add(List.of(
                    "Mesero",
                    text(waiter.meseroId()),
                    text(waiter.mesero()),
                    text(waiter.cantidadVentas()),
                    text(waiter.montoVendido())
            ));
        }

        return new ReportExportData(
                "Reporte de ventas",
                List.of(
                        period(startDate, endDate),
                        "Cantidad de ventas: "
                                + report.cantidadVentas(),
                        "Monto total vendido: "
                                + report.montoTotalVendido()
                ),
                List.of(
                        "Tipo",
                        "ID",
                        "Nombre",
                        "Cantidad",
                        "Monto"
                ),
                rows
        );
    }

    private ReportExportData buildDishSales(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        DishSalesReportResponse report =
                dishSalesReportService.getReport(
                        startDate,
                        endDate,
                        authentication
                );

        List<List<String>> rows =
                new ArrayList<>();

        for (DishSalesReportItemResponse dish :
                report.masVendidos()) {

            rows.add(List.of(
                    "Mas vendido",
                    text(dish.platilloId()),
                    text(dish.codigo()),
                    text(dish.nombre()),
                    text(dish.categoria()),
                    text(dish.cantidadVendida())
            ));
        }

        for (DishSalesReportItemResponse dish :
                report.menosVendidos()) {

            rows.add(List.of(
                    "Menos vendido",
                    text(dish.platilloId()),
                    text(dish.codigo()),
                    text(dish.nombre()),
                    text(dish.categoria()),
                    text(dish.cantidadVendida())
            ));
        }

        return new ReportExportData(
                "Reporte de platillos mas y menos vendidos",
                List.of(
                        period(startDate, endDate),
                        "Hubo ventas: "
                                + report.huboVentas()
                ),
                List.of(
                        "Clasificacion",
                        "ID",
                        "Codigo",
                        "Platillo",
                        "Categoria",
                        "Cantidad"
                ),
                rows
        );
    }

    private ReportExportData buildCurrentProfitability(
            Authentication authentication) {

        List<CurrentDishProfitabilityResponse> report =
                profitabilityReportService
                        .getCurrentProfitability(
                                authentication
                        );

        List<List<String>> rows =
                report.stream()
                        .map(dish -> List.of(
                                text(dish.platilloId()),
                                text(dish.codigo()),
                                text(dish.platillo()),
                                text(dish.categoria()),
                                text(dish.precioVenta()),
                                text(dish.costoProduccion()),
                                text(dish.gananciaUnitaria()),
                                text(dish.margenRentabilidad()),
                                text(dish.rentabilidadCalculable()),
                                text(dish.motivoNoCalculable())
                        ))
                        .toList();

        return new ReportExportData(
                "Reporte de rentabilidad actual por platillo",
                List.of(
                        "Valores actuales de precio y costo de receta"
                ),
                List.of(
                        "ID",
                        "Codigo",
                        "Platillo",
                        "Categoria",
                        "Precio venta",
                        "Costo produccion",
                        "Ganancia",
                        "Margen %",
                        "Calculable",
                        "Observacion"
                ),
                rows
        );
    }

    private ReportExportData buildHistoricalProfitability(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        HistoricalProfitabilityReportResponse report =
                profitabilityReportService
                        .getHistoricalProfitability(
                                startDate,
                                endDate,
                                authentication
                        );

        List<List<String>> rows =
                report.platillos()
                        .stream()
                        .map(dish -> List.of(
                                text(dish.platilloId()),
                                text(dish.platillo()),
                                text(dish.cantidadVendida()),
                                text(dish.ingresoHistorico()),
                                text(dish.costoHistorico()),
                                text(dish.gananciaHistorica()),
                                text(dish.margenRentabilidad())
                        ))
                        .toList();

        return new ReportExportData(
                "Reporte historico de rentabilidad",
                List.of(
                        period(startDate, endDate),
                        "Hubo ventas: "
                                + report.huboVentas()
                ),
                List.of(
                        "ID",
                        "Platillo",
                        "Cantidad",
                        "Ingreso",
                        "Costo",
                        "Ganancia",
                        "Margen %"
                ),
                rows
        );
    }

    private ReportExportData buildTableOccupancy(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        TableOccupancyReportResponse report =
                tableOccupancyReportService.getReport(
                        startDate,
                        endDate,
                        authentication
                );

        List<List<String>> rows =
                report.ocupacionPorHorario()
                        .stream()
                        .map(slot -> List.of(
                                text(slot.fecha()),
                                text(slot.hora()),
                                text(slot.mesasOcupadas())
                        ))
                        .toList();

        return new ReportExportData(
                "Reporte de ocupacion de mesas",
                List.of(
                        period(startDate, endDate),
                        "Hubo ocupaciones: "
                                + report.huboOcupaciones()
                ),
                List.of(
                        "Fecha",
                        "Hora",
                        "Mesas ocupadas"
                ),
                rows
        );
    }

    private ReportExportData buildWaiterPerformance(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        WaiterPerformanceReportResponse report =
                waiterPerformanceReportService
                        .getReport(
                                startDate,
                                endDate,
                                authentication
                        );

        List<List<String>> rows =
                report.meseros()
                        .stream()
                        .map(waiter -> List.of(
                                text(waiter.meseroId()),
                                text(waiter.mesero()),
                                text(waiter.cantidadVentas()),
                                text(waiter.montoTotalVendido()),
                                text(waiter.cantidadCalificaciones()),
                                text(waiter.calificacionPromedio())
                        ))
                        .toList();

        return new ReportExportData(
                "Reporte de desempeno de meseros",
                List.of(
                        period(startDate, endDate),
                        "Hubo datos: "
                                + report.huboDatos()
                ),
                List.of(
                        "ID",
                        "Mesero",
                        "Ventas",
                        "Monto vendido",
                        "Calificaciones",
                        "Promedio"
                ),
                rows
        );
    }

    private ReportExportData buildLoyalty(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        LoyaltyReportResponse report =
                loyaltyReportService.getReport(
                        startDate,
                        endDate,
                        authentication
                );

        List<List<String>> rows =
                report.clientesFrecuentes()
                        .stream()
                        .map(customer -> List.of(
                                text(customer.clienteId()),
                                text(customer.nombres()),
                                text(customer.apellidos()),
                                text(customer.cantidadVisitas())
                        ))
                        .toList();

        return new ReportExportData(
                "Reporte de fidelizacion",
                List.of(
                        period(startDate, endDate),
                        "Puntos otorgados: "
                                + report.puntosOtorgados(),
                        "Puntos redimidos: "
                                + report.puntosRedimidos()
                ),
                List.of(
                        "Cliente ID",
                        "Nombres",
                        "Apellidos",
                        "Cantidad de visitas"
                ),
                rows
        );
    }

    private ReportExportData buildInventory(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        InventoryReportResponse report =
                inventoryReportService.getInventoryReport(
                        startDate,
                        endDate,
                        authentication
                );

        List<List<String>> rows = new ArrayList<>();

        for (InventorySupplyValuationResponse item : report.valoracionInventario()) {
            rows.add(List.of(
                    text(item.codigo()),
                    text(item.nombre()),
                    text(item.categoria()),
                    text(item.stockActual()),
                    text(item.stockMinimo()),
                    text(item.unidadMedida()),
                    item.costoUnitario() != null ? text(item.costoUnitario()) : "N/D",
                    item.valorTotal() != null ? text(item.valorTotal()) : "Incalculable",
                    item.valorCalculable() ? "Calculado" : "Sin costo registrado"
            ));
        }

        List<String> criteria = new ArrayList<>();
        if (startDate != null && endDate != null) {
            criteria.add(period(startDate, endDate));
        }
        criteria.add("Total de Insumos: " + report.totalInsumos());
        criteria.add("Insumos con Stock Bajo: " + report.totalInsumosBajoStock());
        criteria.add("Insumos sin Costo Valido: " + report.totalInsumosSinCosto());
        criteria.add("Valor Total del Inventario: " + text(report.valorTotalInventario())
                + (report.valorTotalCompleto() ? " (Completo)" : " (Incompleto)"));

        if (!report.valorTotalCompleto() && report.advertenciaValorIncompleto() != null) {
            criteria.add("Advertencia: " + report.advertenciaValorIncompleto());
        }

        if (report.seccionMermas() != null && report.seccionMermas().seccionGenerada()) {
            criteria.add("Total Registros Mermas: " + report.seccionMermas().totalRegistros());
            criteria.add("Costo Total Mermas: " + text(report.seccionMermas().costoTotal()));
        }

        return new ReportExportData(
                "Reporte Consolidado de Inventario",
                criteria,
                List.of(
                        "Codigo",
                        "Insumo",
                        "Categoria",
                        "Stock Actual",
                        "Stock Minimo",
                        "Unidad",
                        "Costo Unitario",
                        "Valor Total",
                        "Estado Valoracion"
                ),
                rows
        );
    }

    private void requirePeriod(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "report_period_required",
                    "Periodo requerido",
                    "Debe indicar fechaInicio y fechaFin para exportar este reporte"
            );
        }
    }

    private String period(
            LocalDate startDate,
            LocalDate endDate) {

        return "Periodo: "
                + startDate
                + " a "
                + endDate;
    }

    private String buildFileName(
            ReportType type,
            LocalDate startDate,
            LocalDate endDate,
            String extension) {

        String base =
                "reporte-"
                        + type.name()
                        .toLowerCase()
                        .replace('_', '-');

        if (startDate != null && endDate != null) {
            base += "-"
                    + startDate
                    + "-"
                    + endDate;
        }

        return base + "." + extension;
    }

    private String text(Object value) {
        return value == null
                ? ""
                : value.toString();
    }
}