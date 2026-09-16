package com.restaurante.web.cash;

import com.restaurante.application.payment.ServiceRatingService;
import com.restaurante.web.dto.rating.ServiceRatingRequest;
import com.restaurante.web.dto.rating.ServiceRatingResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caja/facturas")
@SecurityRequirement(name = "bearerAuth")
public class ServiceRatingController {

    private final ServiceRatingService serviceRatingService;

    public ServiceRatingController(
            ServiceRatingService serviceRatingService) {

        this.serviceRatingService = serviceRatingService;
    }

    @PostMapping("/{facturaId}/calificacion")
    public ResponseEntity<ServiceRatingResponse> registerRating(
            @PathVariable Long facturaId,
            @Valid @RequestBody ServiceRatingRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        serviceRatingService.registerRating(
                                facturaId,
                                request
                        )
                );
    }
}