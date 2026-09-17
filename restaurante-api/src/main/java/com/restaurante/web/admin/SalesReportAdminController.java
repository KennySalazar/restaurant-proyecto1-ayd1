package com.restaurante.web.admin;

import com.restaurante.application.report.SalesReportService;
import com.restaurante.web.dto.report.SalesReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/ventas")
@SecurityRequirement(name = "bearerAuth")
public class SalesReportAdminController {

    private final SalesReportService salesReportService;

    public SalesReportAdminController(
            SalesReportService salesReportService) {

        this.salesReportService = salesReportService;
    }

    @GetMapping
    public ResponseEntity<SalesReportResponse> getReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                salesReportService.getReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}