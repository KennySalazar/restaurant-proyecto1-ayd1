package com.restaurante.web.admin;

import com.restaurante.application.report.TableOccupancyReportService;
import com.restaurante.web.dto.report.TableOccupancyReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/ocupacion-mesas")
@SecurityRequirement(name = "bearerAuth")
public class TableOccupancyReportAdminController {

    private final TableOccupancyReportService tableOccupancyReportService;

    public TableOccupancyReportAdminController(
            TableOccupancyReportService tableOccupancyReportService) {

        this.tableOccupancyReportService =
                tableOccupancyReportService;
    }

    @GetMapping
    public ResponseEntity<TableOccupancyReportResponse> getReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                tableOccupancyReportService.getReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}