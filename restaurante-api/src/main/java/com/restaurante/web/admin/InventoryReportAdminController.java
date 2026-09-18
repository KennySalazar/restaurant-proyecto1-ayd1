package com.restaurante.web.admin;

import com.restaurante.application.report.InventoryReportService;
import com.restaurante.web.dto.report.InventoryReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/inventario")
@SecurityRequirement(name = "bearerAuth")
public class InventoryReportAdminController {

    private final InventoryReportService inventoryReportService;

    public InventoryReportAdminController(
            InventoryReportService inventoryReportService) {
        this.inventoryReportService = inventoryReportService;
    }

    @GetMapping
    public ResponseEntity<InventoryReportResponse> getReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                inventoryReportService.getInventoryReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}
