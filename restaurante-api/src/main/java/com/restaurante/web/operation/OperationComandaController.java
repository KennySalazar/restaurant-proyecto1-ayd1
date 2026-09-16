package com.restaurante.web.operation;

import com.restaurante.application.inventory.ComandaInventoryService;
import com.restaurante.domain.model.ComandaDetailStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.comanda.CancelUnsentDishResponse;
import com.restaurante.web.dto.comanda.ComandaDishProgressResponse;
import com.restaurante.web.dto.comanda.ComandaInventoryProcessResponse;
import com.restaurante.web.dto.comanda.ComandaResponse;
import com.restaurante.web.dto.comanda.CreateComandaRequest;
import com.restaurante.web.dto.comanda.DeliverDishRequest;
import com.restaurante.web.dto.comanda.DishCancellationExceptionResponse;
import com.restaurante.web.dto.comanda.RegisterDishCancellationRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador para la gestión operativa de comandas y descuento de inventario.
 */
@RestController
@RequestMapping("/operacion/comandas")
@Tag(name = "Comandas (Operación)", description = "Operaciones de registro, envío y descuento de inventario por comandas")
@SecurityRequirement(name = "bearerAuth")
public class OperationComandaController {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ComandaInventoryService comandaInventoryService;
    private final RestaurantUserProfileRepository userProfileRepository;

    public OperationComandaController(
            ComandaInventoryService comandaInventoryService,
            RestaurantUserProfileRepository userProfileRepository) {
        this.comandaInventoryService = comandaInventoryService;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Registra una comanda para una cuenta/mesa. Opcionalmente puede enviarse de inmediato.
     */
    @PostMapping
    @Operation(summary = "Registrar comanda de operación")
    public ResponseEntity<ComandaInventoryProcessResponse> createComanda(
            @Valid @RequestBody CreateComandaRequest request,
            Authentication authentication) {

        ComandaInventoryProcessResponse response = comandaInventoryService.createComanda(request, authentication);
        HttpStatus status = Boolean.TRUE.equals(request.sendImmediately()) ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Envía una comanda registrada en borrador a cocina y descuenta automáticamente los insumos en el kardex.
     */
    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Enviar comanda y descontar insumos automáticamente",
            description = "Valida y descuenta automáticamente los insumos de inventario de cada platillo y modificador. Notifica a cocina en tiempo real y marca los platillos como recibidos. Si algún platillo no tiene stock suficiente, rechaza su envío, lo marca como no disponible y notifica al mesero."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Comanda enviada a cocina exitosamente",
                    content = @Content(schema = @Schema(implementation = ComandaInventoryProcessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Comanda vacía, sin platillos en borrador o stock insuficiente de insumos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La comanda ya fue procesada previamente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComandaInventoryProcessResponse> sendComanda(
            @PathVariable Long id,
            Authentication authentication) {

        ComandaInventoryProcessResponse response = comandaInventoryService.sendComanda(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta una comanda por su identificador.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar comanda por identificador")
    public ResponseEntity<ComandaResponse> getComandaById(@PathVariable Long id) {
        ComandaResponse response = comandaInventoryService.getComandaById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/platillos/avance")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN', 'KITCHEN')")
    @Operation(
            summary = "Visualizar avance de platillos ordenados",
            description = "Permite al mesero consultar el estado y avance de preparación de cada platillo ordenado (filtrando opcionalmente por mesa, cuenta, comanda o estado) para llevar control de lo servido en la mesa."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de platillos y su estado de avance obtenido exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ComandaDishProgressResponse.class)))
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
    public ResponseEntity<List<ComandaDishProgressResponse>> getDishProgress(
            @Parameter(description = "Filtro opcional por mesa")
            @RequestParam(required = false) Long mesaId,
            @Parameter(description = "Filtro opcional por cuenta")
            @RequestParam(required = false) Long cuentaId,
            @Parameter(description = "Filtro opcional por comanda")
            @RequestParam(required = false) Long comandaId,
            @Parameter(description = "Filtro opcional por estado del platillo (BORRADOR, RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO, etc.)")
            @RequestParam(required = false) ComandaDetailStatus estado,
            @Parameter(description = "Filtro opcional por identificador de mesero asignado")
            @RequestParam(required = false) Long meseroId,
            @Parameter(description = "Filtro opcional para obtener únicamente platillos que superaron su tiempo estimado sin estar listos")
            @RequestParam(required = false) Boolean soloRetrasados,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        List<ComandaDishProgressResponse> response = comandaInventoryService.getDishProgress(
                restaurantId, mesaId, cuentaId, comandaId, estado, meseroId, soloRetrasados
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alertas/tiempo-excedido")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar alertas de platillos con tiempo de preparación excedido",
            description = "Consulta y resalta los platillos de las mesas asignadas (o de todo el restaurante) que han superado su tiempo estimado de preparación sin haber sido marcados como listos, para priorizar su seguimiento y anticipar reclamos del cliente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de platillos con tiempo de preparación excedido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ComandaDishProgressResponse.class)))
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
    public ResponseEntity<List<ComandaDishProgressResponse>> getDelayedDishAlerts(
            @Parameter(description = "Filtro opcional por identificador de mesero")
            @RequestParam(required = false) Long meseroId,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        Long targetWaiterId = meseroId;
        if (targetWaiterId == null && isWaiterUser(authentication)) {
            targetWaiterId = resolveUserId(authentication);
        }
        List<ComandaDishProgressResponse> response = comandaInventoryService.getDelayedDishAlerts(restaurantId, targetWaiterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/avance")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN', 'KITCHEN')")
    @Operation(
            summary = "Visualizar avance de platillos de una comanda",
            description = "Consulta el detalle y avance de todos los platillos ordenados en una comanda específica."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Avance de platillos de la comanda obtenido exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ComandaDishProgressResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<ComandaDishProgressResponse>> getComandaDishProgress(
            @Parameter(description = "Identificador de la comanda", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        List<ComandaDishProgressResponse> response = comandaInventoryService.getComandaDishProgress(id, restaurantId);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/platillos/{id}/entregar", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Marcar platillo como entregado en la mesa",
            description = "Marca un platillo de una comanda en estado LISTO como ENTREGADO cuando el mesero lo sirve en la mesa. Registra el momento de entrega, el mesero responsable y sincroniza la comanda."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo marcado como entregado exitosamente",
                    content = @Content(schema = @Schema(implementation = ComandaDishProgressResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El platillo no se encuentra en estado LISTO para ser entregado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComandaDishProgressResponse> markDishAsDelivered(
            @Parameter(description = "Identificador del detalle de la comanda (platillo)", required = true)
            @PathVariable Long id,
            @RequestBody(required = false) DeliverDishRequest request,
            Authentication authentication) {

        ComandaDishProgressResponse response = comandaInventoryService.markDishAsDelivered(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/platillos/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Cancelar platillo no enviado a cocina",
            description = "Elimina un platillo de una comanda antes de enviarla a cocina para corregir errores de pedido sin afectar el inventario ni cocina. Si el platillo ya fue enviado (estado RECIBIDO o posterior), se impide la eliminación directa indicando remitirse al flujo de cancelación excepcional."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo eliminado exitosamente de la comanda en borrador",
                    content = @Content(schema = @Schema(implementation = CancelUnsentDishResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de platillo inválido o no especificado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya fue enviado a cocina (requiere flujo de cancelación excepcional) o la cuenta se encuentra en proceso de cobro",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<CancelUnsentDishResponse> cancelUnsentDish(
            @Parameter(description = "Identificador único del detalle de comanda (platillo) a cancelar", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        CancelUnsentDishResponse response = comandaInventoryService.cancelUnsentDish(id, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/platillos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Registrar excepción de cancelación de platillo en preparación",
            description = "Registra la cancelación de un platillo que ya ha sido enviado a cocina o se encuentra en preparación como una excepción autorizada. Registra el motivo, responsable solicitante, supervisor/administrador autorizador y la acción sobre el inventario (merma o reintegro)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cancelación excepcional registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = DishCancellationExceptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Motivo no especificado, platillo aún en borrador o datos inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya fue entregado, ya está cancelado, o la cuenta está en cobro",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishCancellationExceptionResponse> registerDishCancellationException(
            @Parameter(description = "Identificador único del detalle de comanda (platillo) a cancelar como excepción", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RegisterDishCancellationRequest request,
            Authentication authentication) {

        DishCancellationExceptionResponse response = comandaInventoryService.registerDishCancellationException(id, request, authentication);
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

    private Long resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.userId();
        }
        return null;
    }

    private boolean isWaiterUser(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.role() == RoleName.WAITER;
        }
        return false;
    }
}
