package com.restaurante.web.admin;

import com.restaurante.application.waitlist.WaitlistService;
import com.restaurante.web.dto.waitlist.CreateWaitlistEntryRequest;
import com.restaurante.web.dto.waitlist.WaitlistEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.restaurante.web.dto.waitlist.WaitlistQueueEntryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/admin/lista-espera")
@Tag(
        name = "Lista de espera",
        description = "Gestión administrativa de la lista de espera"
)
@SecurityRequirement(name = "bearerAuth")
public class WaitlistAdminController {

    private final WaitlistService waitlistService;

    public WaitlistAdminController(
            WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un cliente a la lista de espera")
    public WaitlistEntryResponse createWaitlistEntry(
            @Valid @RequestBody CreateWaitlistEntryRequest request,
            Authentication authentication) {

        return waitlistService.createWaitlistEntry(
                request,
                authentication
        );
    }
    @GetMapping
    @Operation(summary = "Consultar la lista de espera")
    public List<WaitlistQueueEntryResponse> getWaitlist(
            Authentication authentication) {

        return waitlistService.getWaitlist(
                authentication
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un cliente de la lista de espera")
    public WaitlistQueueEntryResponse getWaitlistEntry(
            @PathVariable Long id,
            Authentication authentication) {

        return waitlistService.getWaitlistEntry(
                id,
                authentication
        );
    }
}