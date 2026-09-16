package com.restaurante.web.admin;

import com.restaurante.application.report.DishSalesReportService;
import com.restaurante.web.dto.report.DishSalesReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/platillos-ventas")
@SecurityRequirement(name = "bearerAuth")
public class DishSalesReportAdminController {

    private final DishSalesReportService dishSalesReportService;

    public DishSalesReportAdminController(
            DishSalesReportService dishSalesReportService) {

        this.dishSalesReportService =
                dishSalesReportService;
    }

    @GetMapping
    public ResponseEntity<DishSalesReportResponse> getReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                dishSalesReportService.getReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}