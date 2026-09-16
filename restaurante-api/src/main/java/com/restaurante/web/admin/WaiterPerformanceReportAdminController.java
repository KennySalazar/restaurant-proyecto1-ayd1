package com.restaurante.web.admin;

import com.restaurante.application.report.WaiterPerformanceReportService;
import com.restaurante.web.dto.report.WaiterPerformanceReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/desempeno-meseros")
@SecurityRequirement(name = "bearerAuth")
public class WaiterPerformanceReportAdminController {

    private final WaiterPerformanceReportService service;

    public WaiterPerformanceReportAdminController(
            WaiterPerformanceReportService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<WaiterPerformanceReportResponse> getReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                service.getReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}