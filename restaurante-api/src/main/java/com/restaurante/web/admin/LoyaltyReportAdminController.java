package com.restaurante.web.admin;

import com.restaurante.application.report.LoyaltyReportService;
import com.restaurante.web.dto.report.LoyaltyReportResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reportes/fidelizacion")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyReportAdminController {

    private final LoyaltyReportService loyaltyReportService;

    public LoyaltyReportAdminController(
            LoyaltyReportService loyaltyReportService) {

        this.loyaltyReportService =
                loyaltyReportService;
    }

    @GetMapping
    public ResponseEntity<LoyaltyReportResponse> getReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication) {

        return ResponseEntity.ok(
                loyaltyReportService.getReport(
                        fechaInicio,
                        fechaFin,
                        authentication
                )
        );
    }
}