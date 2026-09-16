package com.restaurante.web.admin;

import com.restaurante.application.report.ProfitabilityReportService;
import com.restaurante.web.dto.report.CurrentDishProfitabilityResponse;
import com.restaurante.web.dto.report.HistoricalProfitabilityReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/reportes/rentabilidad")
@SecurityRequirement(name = "bearerAuth")
public class ProfitabilityReportAdminController {

    private final ProfitabilityReportService profitabilityReportService;

    public ProfitabilityReportAdminController(
            ProfitabilityReportService profitabilityReportService) {

        this.profitabilityReportService =
                profitabilityReportService;
    }

    @GetMapping("/actual")
    public ResponseEntity<List<CurrentDishProfitabilityResponse>>
    getCurrentProfitability(
            Authentication authentication) {

        return ResponseEntity.ok(
                profitabilityReportService
                        .getCurrentProfitability(authentication)
        );
    }

    @GetMapping("/historico")
    public ResponseEntity<HistoricalProfitabilityReportResponse>
    getHistoricalProfitability(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                profitabilityReportService
                        .getHistoricalProfitability(
                                fechaInicio,
                                fechaFin,
                                authentication
                        )
        );
    }
}