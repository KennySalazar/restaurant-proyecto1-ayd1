package com.restaurante.application.report.export;

import java.util.List;

public record ReportExportData(
        String titulo,
        List<String> criterios,
        List<String> encabezados,
        List<List<String>> filas
) {
}