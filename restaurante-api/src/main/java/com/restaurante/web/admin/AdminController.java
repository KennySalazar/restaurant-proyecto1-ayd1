package com.restaurante.web.admin;

import com.restaurante.web.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Tag(name = "Administración", description = "Operaciones restringidas al rol ADMIN")
public class AdminController {

    @GetMapping("/ping")
    @Operation(summary = "Comprobar autorización de administrador",
        security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse ping() {
        return new MessageResponse("Acceso de administrador concedido");
    }
}
