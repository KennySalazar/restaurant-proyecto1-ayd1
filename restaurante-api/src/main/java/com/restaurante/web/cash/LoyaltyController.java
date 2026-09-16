package com.restaurante.web.cash;

import com.restaurante.application.payment.LoyaltyService;
import com.restaurante.web.dto.payment.CustomerPointsResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/caja/clientes")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(
            LoyaltyService loyaltyService) {

        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/{clienteId}/puntos")
    public ResponseEntity<CustomerPointsResponse> getCustomerPoints(
            @PathVariable Long clienteId,
            Authentication authentication) {

        return ResponseEntity.ok(
                loyaltyService.getCustomerPoints(
                        clienteId,
                        authentication
                )
        );
    }
}