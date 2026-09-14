package com.restaurante.web.admin;

import com.restaurante.application.modifier.ModifierService;
import com.restaurante.web.dto.modifier.AssociateDishesRequest;
import com.restaurante.web.dto.modifier.CreateModifierRequest;
import com.restaurante.web.dto.modifier.ModifierDeactivationResponse;
import com.restaurante.web.dto.modifier.ModifierRegistrationResponse;
import com.restaurante.web.dto.modifier.ModifierResponse;
import com.restaurante.web.dto.modifier.ModifierUpdateResponse;
import com.restaurante.web.dto.modifier.UpdateModifierRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la gestión de modificadores de platillos.
 */
@RestController
@RequestMapping("/admin/modifiers")
@Tag(name = "Modificadores de Platillos (Administración)", description = "Operaciones administrativas para gestionar modificadores y opciones de personalización de platillos")
@SecurityRequirement(name = "bearerAuth")
public class AdminModifierController {

    private final ModifierService modifierService;

    public AdminModifierController(ModifierService modifierService) {
        this.modifierService = modifierService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar un nuevo modificador de platillo",
            description = "Registra un modificador con su nombre comercial, costo adicional (o sin costo adicional) y lo asocia al menos con un platillo existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Modificador registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nombre vacío, costo adicional negativo o lista de platillos vacía",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Uno o más platillos asociados no existen",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Nombre o código de modificador duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierRegistrationResponse> registerModifier(
            @Valid @RequestBody CreateModifierRequest request
    ) {
        ModifierRegistrationResponse response = modifierService.registerModifier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Consultar el catálogo de modificadores",
            description = "Retorna el listado de modificadores registrados con sus platillos asociados. Permite filtrar por platillo, búsqueda de texto y estado activo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de modificadores obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<ModifierResponse>> listModifiers(
            @Parameter(description = "Identificador de platillo para filtrar modificadores asociados (opcional)")
            @RequestParam(required = false) Long dishId,
            @Parameter(description = "Término de búsqueda por nombre o código (opcional)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por estado activo (opcional)")
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(modifierService.listModifiers(dishId, search, active));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar detalle de un modificador por ID",
            description = "Retorna la información consolidada de un modificador y la lista de platillos asociados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Modificador encontrado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierResponse> getModifierById(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(modifierService.getModifierById(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un modificador",
            description = "Actualiza el nombre comercial, costo adicional, descripción o platillos asociados de un modificador registrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Modificador actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nombre vacío, costo adicional negativo o lista de platillos vacía",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador o platillo asociado no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Nombre o código duplicado en otro modificador",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierUpdateResponse> updateModifier(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateModifierRequest request
    ) {
        return ResponseEntity.ok(modifierService.updateModifier(id, request));
    }

    @PutMapping("/{id}/dishes")
    @Operation(
            summary = "Asociar un modificador con varios platillos",
            description = "Asocia un modificador activo con los platillos seleccionados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillos asociados al modificador exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Lista de platillos vacía o no especificada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El modificador se encuentra inactivo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierUpdateResponse> associateDishes(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AssociateDishesRequest request
    ) {
        return ResponseEntity.ok(modifierService.associateDishes(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desactivar un modificador",
            description = "Desactiva un modificador impidiendo su selección en nuevas comandas y conservándolo intacto en registros históricos de ventas anteriores."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Modificador desactivado exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierDeactivationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El modificador ya se encuentra desactivado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierDeactivationResponse> deactivateModifier(
            @Parameter(description = "Identificador único del modificador a desactivar", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(modifierService.deactivateModifier(id));
    }
}
