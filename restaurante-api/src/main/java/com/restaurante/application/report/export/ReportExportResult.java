package com.restaurante.application.report.export;

public record ReportExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
}