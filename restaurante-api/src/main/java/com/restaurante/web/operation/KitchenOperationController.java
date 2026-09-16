package com.restaurante.web.operation;

import com.restaurante.application.kitchen.KitchenService;
import com.restaurante.domain.model.ComandaStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.kitchen.DishPreparationStatusResponse;
import com.restaurante.web.dto.kitchen.DishUnavailableResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import com.restaurante.web.dto.kitchen.MarkDishUnavailableRequest;
import com.restaurante.web.dto.kitchen.UpdateDishPreparationStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controlador de operación para la visualización y gestión de comandas en cocina.
 */
@RestController
@RequestMapping("/operacion/cocina")
@Tag(
        name = "Cocina (Operación)",
        description = "Visualización de comandas entrantes ordenadas por antigüedad, actualización de estados de preparación y sincronización en tiempo real para el personal de cocina"
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

    @GetMapping("/comandas")
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
            @Parameter(description = "Filtro opcional para obtener únicamente comandas con tiempo de preparación excedido")
            @RequestParam(required = false) Boolean soloRetrasadas,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        List<KitchenComandaResponse> response = kitchenService.getActiveKitchenComandas(restaurantId, status, soloRetrasadas);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alertas/tiempo-excedido")
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'WAITER')")
    @Operation(
            summary = "Visualizar alertas de comandas y platillos con tiempo de preparación excedido en cocina",
            description = "Consulta las comandas activas en cocina que contienen platillos que han superado su tiempo estimado de preparación sin estar listos, permitiendo priorizar su atención para anticipar reclamos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de comandas activas con tiempo de preparación excedido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = KitchenComandaResponse.class)))
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
    public ResponseEntity<List<KitchenComandaResponse>> getDelayedKitchenComandas(Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);
        List<KitchenComandaResponse> response = kitchenService.getDelayedKitchenComandas(restaurantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/comandas/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'WAITER')")
    @Operation(
            summary = "Suscripción en tiempo real a comandas de cocina (SSE)",
            description = "Conexión mediante Server-Sent Events (SSE) que emite eventos instantáneos al recibir nuevas comandas o actualizar estados sin que sea necesario recargar la página."
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

    @GetMapping("/comandas/{id}")
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

    @RequestMapping(value = "/platillos/{id}/estado", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    @Operation(
            summary = "Actualizar estado de preparación de un platillo",
            description = "Actualiza el estado de preparación de un platillo de una comanda (recibido → en preparación → listo). Valida la secuencia de transición (no permite saltar de recibido a listo). Notifica al mesero responsable cuando el platillo queda listo para servir y sincroniza el estado en tiempo real."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado de preparación del platillo actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = DishPreparationStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Transición de estado inválida o datos de solicitud incompletos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de cocina o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishPreparationStatusResponse> updateDishPreparationStatus(
            @Parameter(description = "Identificador único del detalle de la comanda (platillo)", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateDishPreparationStatusRequest request,
            Authentication authentication) {

        DishPreparationStatusResponse response = kitchenService.updateDishPreparationStatus(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/platillos/{id}/no-disponible", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    @Operation(
            summary = "Marcar platillo como no disponible por falta de insumo o discrepancia",
            description = "Marca un platillo en curso como no disponible cuando cocina detecta falta de insumo o discrepancia de inventario al cocinar. Notifica automáticamente al mesero asignado para informar al cliente y desactiva la disponibilidad del platillo en el menú para prevenir nuevas órdenes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo marcado como no disponible exitosamente y mesero notificado",
                    content = @Content(schema = @Schema(implementation = DishUnavailableResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El platillo no puede marcarse como no disponible en su estado actual",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de cocina o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishUnavailableResponse> markDishAsUnavailable(
            @Parameter(description = "Identificador único del detalle de la comanda (platillo)", required = true)
            @PathVariable Long id,
            @RequestBody(required = false) MarkDishUnavailableRequest request,
            Authentication authentication) {

        DishUnavailableResponse response = kitchenService.markDishAsUnavailable(id, request, authentication);
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
