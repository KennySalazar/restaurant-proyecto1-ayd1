package com.restaurante.web.operation;

import com.restaurante.application.kitchen.KitchenService;
import com.restaurante.domain.model.ComandaStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controlador de operación para la visualización y gestión de comandas en cocina.
 */
@RestController
@RequestMapping("/operacion/cocina/comandas")
@Tag(
        name = "Cocina (Operación)",
        description = "Visualización de comandas entrantes ordenadas por antigüedad y actualización en tiempo real para el personal de cocina"
)
@SecurityRequirement(name = "bearerAuth")
public class KitchenOperationController {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final KitchenService kitchenService;
    private final RestaurantUserProfileRepository userProfileRepository;

    public KitchenOperationController(KitchenService kitchenService,
                                      RestaurantUserProfileRepository userProfileRepository) {
        this.kitchenService = kitchenService;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'WAITER')")
    @Operation(
            summary = "Visualizar comandas entrantes y activas en cocina",
            description = "Consulta las comandas activas en cocina ordenadas por antigüedad (de la más antigua a la más reciente), incluyendo detalle de platillos, cantidades, modificadores, notas y tiempo estimado de preparación para priorizar órdenes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de comandas en cocina ordenadas por antigüedad",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = KitchenComandaResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de cocina, administrador o mesero)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<KitchenComandaResponse>> getActiveKitchenComandas(
            @Parameter(description = "Filtro opcional por estado de la comanda (RECIBIDA, EN_PREPARACION)")
            @RequestParam(required = false) ComandaStatus status,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        List<KitchenComandaResponse> response = kitchenService.getActiveKitchenComandas(restaurantId, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    @Operation(
            summary = "Suscripción en tiempo real a comandas de cocina (SSE)",
            description = "Conexión mediante Server-Sent Events (SSE) que emite eventos instantáneos al recibir nuevas comandas sin que sea necesario recargar la página."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Canal SSE establecido exitosamente",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public SseEmitter subscribeToKitchenStream(Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);
        return kitchenService.subscribeToKitchenStream(restaurantId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'WAITER')")
    @Operation(
            summary = "Consultar comanda de cocina por identificador",
            description = "Devuelve los datos operativos completos de una comanda específica para cocina."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la comanda para cocina",
                    content = @Content(schema = @Schema(implementation = KitchenComandaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<KitchenComandaResponse> getKitchenComandaById(
            @Parameter(description = "Identificador único de la comanda", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        KitchenComandaResponse response = kitchenService.getKitchenComandaById(id, restaurantId);
        return ResponseEntity.ok(response);
    }

    private Long resolveRestaurantId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return userProfileRepository.findById(jwtData.userId())
                    .map(RestaurantUserProfile::getRestaurantId)
                    .orElse(DEFAULT_RESTAURANT_ID);
        }
        return DEFAULT_RESTAURANT_ID;
    }
}
