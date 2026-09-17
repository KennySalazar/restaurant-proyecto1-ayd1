package com.restaurante.web.cash;

import com.restaurante.application.cash.CashShiftService;
import com.restaurante.web.dto.cash.CashRegisterAvailabilityResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/caja/cajas")
@SecurityRequirement(name = "bearerAuth")
public class CashRegisterController {

    private final CashShiftService cashShiftService;

    public CashRegisterController(
            CashShiftService cashShiftService) {

        this.cashShiftService = cashShiftService;
    }

    @GetMapping
    public ResponseEntity<List<CashRegisterAvailabilityResponse>>
    getCashRegisters(Authentication authentication) {

        return ResponseEntity.ok(
                cashShiftService.getCashRegisters(authentication)
        );
    }
}