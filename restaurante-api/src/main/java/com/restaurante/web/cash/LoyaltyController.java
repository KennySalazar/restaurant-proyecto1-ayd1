package com.restaurante.web.cash;

import com.restaurante.application.payment.LoyaltyService;
import com.restaurante.web.dto.payment.CustomerPointsResponse;
import com.restaurante.web.dto.payment.CustomerResponse;
import com.restaurante.web.dto.payment.RegisterCustomerRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/caja/clientes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CASHIER')")
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

    @GetMapping("/buscar")
    public ResponseEntity<CustomerResponse> findCustomerByPhone(
            @RequestParam String telefono,
            Authentication authentication) {

        return ResponseEntity.ok(
                loyaltyService.findCustomerByPhone(
                        telefono,
                        authentication
                )
        );
    }

    @PostMapping("/cuentas/{cuentaId}")
    public ResponseEntity<CustomerResponse> registerAndAssociateCustomer(
            @PathVariable Long cuentaId,
            @Valid @RequestBody RegisterCustomerRequest request,
            Authentication authentication) {

        CustomerResponse response =
                loyaltyService.registerAndAssociateCustomer(
                        cuentaId,
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{clienteId}/cuentas/{cuentaId}")
    public ResponseEntity<CustomerResponse> associateCustomer(
            @PathVariable Long clienteId,
            @PathVariable Long cuentaId,
            Authentication authentication) {

        return ResponseEntity.ok(
                loyaltyService.associateCustomer(
                        cuentaId,
                        clienteId,
                        authentication
                )
        );
    }
}
