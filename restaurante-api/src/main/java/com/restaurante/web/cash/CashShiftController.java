package com.restaurante.web.cash;

import com.restaurante.application.cash.CashShiftService;
import com.restaurante.web.dto.cash.CashShiftResponse;
import com.restaurante.web.dto.cash.OpenCashShiftRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caja/turnos")
@Tag(
        name = "Turnos de caja",
        description = "Operaciones de apertura y gestión de turnos de caja"
)
@SecurityRequirement(name = "bearerAuth")
public class CashShiftController {

    private final CashShiftService cashShiftService;

    public CashShiftController(
            CashShiftService cashShiftService) {

        this.cashShiftService = cashShiftService;
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir turno de caja")
    public CashShiftResponse openShift(
            @Valid @RequestBody OpenCashShiftRequest request,
            Authentication authentication) {

        return cashShiftService.openShift(
                request,
                authentication
        );
    }
}