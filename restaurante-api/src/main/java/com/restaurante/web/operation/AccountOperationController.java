package com.restaurante.web.operation;

import com.restaurante.application.account.AccountService;
import com.restaurante.application.account.SubaccountService;
import com.restaurante.application.inventory.ComandaInventoryService;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.MergeAccountsRequest;
import com.restaurante.web.dto.account.MergeAccountsResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
import com.restaurante.web.dto.account.RequestBillResponse;
import com.restaurante.web.dto.account.TransferAccountRequest;
import com.restaurante.web.dto.account.TransferAccountResponse;
import com.restaurante.web.dto.comanda.AddDishResponse;
import com.restaurante.web.dto.comanda.AddDishToAccountRequest;
import com.restaurante.web.dto.subaccount.AssignItemRequest;
import com.restaurante.web.dto.subaccount.SplitAccountResponse;
import com.restaurante.web.dto.subaccount.SplitByItemsRequest;
import com.restaurante.web.dto.subaccount.SplitByPeopleRequest;
import com.restaurante.web.dto.subaccount.SubaccountResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de operación para la apertura, gestión y división de cuentas de mesa para meseros.
 */
@RestController
@RequestMapping("/operacion/cuentas")
@Tag(
        name = "Cuentas (Operación)",
        description = "Apertura, consulta y división de cuentas de mesa para meseros y personal de servicio"
)
@SecurityRequirement(name = "bearerAuth")
public class AccountOperationController {

    private final AccountService accountService;
    private final SubaccountService subaccountService;
    private final ComandaInventoryService comandaInventoryService;

    public AccountOperationController(
            AccountService accountService,
            SubaccountService subaccountService,
            ComandaInventoryService comandaInventoryService) {
        this.accountService = accountService;
        this.subaccountService = subaccountService;
        this.comandaInventoryService = comandaInventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Abrir cuenta en una mesa libre",
            description = "Abre una nueva cuenta asociada a la mesa libre y al mesero autenticado, registrando la hora de apertura y cambiando el estado de la mesa a 'OCUPADA'. Si la mesa ya tiene una cuenta activa, la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cuenta abierta exitosamente y mesa marcada como ocupada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o mesa no especificada",
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
                    description = "Mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa ya tiene una cuenta activa o está bloqueada por reserva",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            Authentication authentication) {

        AccountResponse response = accountService.openAccount(null, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar detalle de una cuenta",
            description = "Devuelve los datos y estado actual de una cuenta específica."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la cuenta consultada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Identificador único de la cuenta", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.getAccountById(id, authentication));
    }

    @GetMapping("/mesa/{mesaId}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar cuenta activa de una mesa",
            description = "Devuelve la cuenta actualmente activa en la mesa indicada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta activa de la mesa",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa no encontrada o no tiene cuenta activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AccountResponse> getActiveAccountByTable(
            @Parameter(description = "Identificador de la mesa", required = true)
            @PathVariable Long mesaId,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.getActiveAccountByTable(mesaId, authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Listar cuentas activas del salón",
            description = "Devuelve todas las cuentas activas (abiertas o en cobro) del restaurante."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de cuentas activas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)))
            )
    })
    public ResponseEntity<List<AccountResponse>> getActiveAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.getActiveAccounts(authentication));
    }

    @PostMapping("/{id}/transferir")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Transferir cuenta a otra mesa libre",
            description = "Transfiere una cuenta abierta a una mesa libre destino, conservando todas las comandas ya registradas, liberando la mesa origen y ocupando la mesa destino. Si la mesa destino no está en estado 'libre' o disponible, la acción es bloqueada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta transferida exitosamente a la mesa libre",
                    content = @Content(schema = @Schema(implementation = TransferAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Mesa destino requerida o idéntica a mesa origen",
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
                    description = "Cuenta o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa destino no está disponible",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<TransferAccountResponse> transferAccount(
            @Parameter(description = "Identificador de la cuenta a transferir", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TransferAccountRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.transferAccountById(id, request, authentication));
    }

    @PostMapping("/fusionar")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Fusionar dos cuentas de mesas",
            description = "Fusiona dos cuentas abiertas de mesas que se unieron físicamente, combinando el detalle y comandas en una sola cuenta y liberando la mesa origen. Si alguna cuenta está en proceso de cobro ('LISTA_COBRO'), la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuentas fusionadas exitosamente y mesa origen liberada",
                    content = @Content(schema = @Schema(implementation = MergeAccountsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuentas requeridas, idénticas o mesas idénticas",
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
                    description = "Cuenta o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya está en proceso de cobro o no está en estado abierta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<MergeAccountsResponse> mergeAccounts(
            @Valid @RequestBody MergeAccountsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.mergeAccounts(null, false, request, authentication));
    }

    @PostMapping("/{id}/fusionar")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Fusionar cuenta origen con otra cuenta destino",
            description = "Fusiona la cuenta abierta especificada por ID con otra cuenta destino indicada en la solicitud, combinando sus órdenes y liberando la mesa origen. Si alguna cuenta está en proceso de cobro ('LISTA_COBRO'), la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuentas fusionadas exitosamente y mesa origen liberada",
                    content = @Content(schema = @Schema(implementation = MergeAccountsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuenta destino requerida, idéntica o misma mesa",
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
                    description = "Cuenta o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya está en proceso de cobro o no está en estado abierta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<MergeAccountsResponse> mergeAccountById(
            @Parameter(description = "Identificador de la cuenta origen a fusionar", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MergeAccountsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.mergeAccountsById(id, request, authentication));
    }

    @PostMapping("/{id}/dividir/personas")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Dividir cuenta por número de personas",
            description = "Divide una cuenta abierta con platillos registrados entre un número de personas especificado, generando sub-cuentas con el porcentaje y subtotal correspondiente a cada una."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta dividida exitosamente entre las personas indicadas",
                    content = @Content(schema = @Schema(implementation = SplitAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Número de personas inválido (mínimo 2) o la cuenta no tiene platillos registrados",
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
                    description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya se encuentra cerrada o tiene subcuentas facturadas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SplitAccountResponse> splitByPeople(
            @Parameter(description = "Identificador de la cuenta abierta a dividir", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SplitByPeopleRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.splitByPeople(id, request, authentication));
    }

    @PostMapping("/{id}/dividir/items")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Dividir cuenta por ítems específicos",
            description = "Divide una cuenta abierta asignando ítems o platillos específicos a distintas sub-cuentas. Cada sub-cuenta reflejará únicamente sus ítems y subtotal. Si se intenta asignar un mismo ítem a más de una sub-cuenta, la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta dividida exitosamente por ítems específicos",
                    content = @Content(schema = @Schema(implementation = SplitAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o platillo no pertenece a la cuenta",
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
                    description = "Cuenta o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya fue asignado a otra sub-cuenta o la cuenta ya tiene subcuentas facturadas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SplitAccountResponse> splitByItems(
            @Parameter(description = "Identificador de la cuenta abierta a dividir", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SplitByItemsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.splitByItems(id, request, authentication));
    }

    @PostMapping("/{id}/subcuentas/{subcuentaId}/items")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Asignar ítem a una subcuenta específica",
            description = "Asigna un platillo de la cuenta a una sub-cuenta existente. Si el platillo ya fue asignado a otra sub-cuenta, el sistema impide la doble asignación."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo asignado exitosamente a la sub-cuenta",
                    content = @Content(schema = @Schema(implementation = SubaccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Platillo ajeno a la cuenta o cantidad inválida",
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
                    description = "Cuenta, subcuenta o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya fue asignado a otra sub-cuenta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SubaccountResponse> assignItemToSubaccount(
            @Parameter(description = "Identificador de la cuenta principal", required = true)
            @PathVariable Long id,
            @Parameter(description = "Identificador de la subcuenta destino", required = true)
            @PathVariable Long subcuentaId,
            @Valid @RequestBody AssignItemRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.assignItemToSubaccount(id, subcuentaId, request, authentication));
    }

    @GetMapping("/{id}/subcuentas")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar subcuentas de una cuenta",
            description = "Devuelve el listado de sub-cuentas generadas para la cuenta, con su detalle de platillos asignados y subtotales."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de subcuentas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubaccountResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<SubaccountResponse>> getSubaccounts(
            @Parameter(description = "Identificador de la cuenta", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.getSubaccountsByAccountId(id, authentication));
    }

    @PostMapping("/{id}/solicitar-cobro")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Marcar cuenta como lista para cobro",
            description = "Marca la cuenta como lista para cobro, cambia el estado de la mesa a 'CUENTA_SOLICITADA' y genera notificación para el cajero. Requiere que la cuenta tenga al menos un platillo registrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta marcada exitosamente como lista para cobro",
                    content = @Content(schema = @Schema(implementation = RequestBillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "La cuenta no tiene platillos registrados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya se encuentra marcada como lista para cobro o no está en estado ABIERTA",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RequestBillResponse> requestBill(
            @Parameter(description = "Identificador único de la cuenta", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.requestBillById(id, authentication));
    }

    @PostMapping("/{id}/platillos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Agregar platillos a una cuenta abierta",
            description = "Registra platillos en la cuenta abierta indicando cantidad, modificadores y notas especiales. Valida que la cuenta esté abierta y que los insumos requeridos por la receta tengan stock suficiente en inventario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Platillo(s) registrado(s) exitosamente en la cuenta",
                    content = @Content(schema = @Schema(implementation = AddDishResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Stock insuficiente de insumos, platillo inactivo o datos inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya se encuentra marcada como lista para cobro o no está en estado ABIERTA",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<AddDishResponse> addDishesToAccount(
            @Parameter(description = "Identificador único de la cuenta", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AddDishToAccountRequest request,
            Authentication authentication) {

        AddDishResponse response = comandaInventoryService.addDishesToAccount(id, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
