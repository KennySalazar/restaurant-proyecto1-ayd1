package com.restaurante.web.admin;

import com.restaurante.application.report.export.ReportExportResult;
import com.restaurante.application.report.export.ReportExportService;
import com.restaurante.web.dto.report.export.ReportExportFormat;
import com.restaurante.web.dto.report.export.ReportType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes")
@SecurityRequirement(name = "bearerAuth")
public class ReportExportAdminController {

    private final ReportExportService reportExportService;

    public ReportExportAdminController(
            ReportExportService reportExportService) {

        this.reportExportService =
                reportExportService;
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> export(
            @RequestParam ReportType tipo,
            @RequestParam ReportExportFormat formato,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        ReportExportResult result =
                reportExportService.export(
                        tipo,
                        formato,
                        fechaInicio,
                        fechaFin,
                        authentication
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + result.fileName()
                                + "\""
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        result.contentType()
                )
                .contentLength(
                        result.content().length
                )
                .body(
                        result.content()
                );
    }
}