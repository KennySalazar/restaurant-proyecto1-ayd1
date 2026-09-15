package com.restaurante.web.operation;

import com.restaurante.application.account.AccountService;
import com.restaurante.application.account.SubaccountService;
import com.restaurante.application.table.TableSeatingService;
import com.restaurante.application.table.TableService;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.MergeAccountsRequest;
import com.restaurante.web.dto.account.MergeAccountsResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
import com.restaurante.web.dto.account.RequestBillResponse;
import com.restaurante.web.dto.account.TransferAccountRequest;
import com.restaurante.web.dto.account.TransferAccountResponse;
import com.restaurante.web.dto.subaccount.SplitAccountResponse;
import com.restaurante.web.dto.subaccount.SplitByItemsRequest;
import com.restaurante.web.dto.subaccount.SplitByPeopleRequest;
import com.restaurante.web.dto.subaccount.SubaccountResponse;
import com.restaurante.web.dto.table.SeatReservationRequest;
import com.restaurante.web.dto.table.SeatReservationResponse;
import com.restaurante.web.dto.table.SeatWaitlistRequest;
import com.restaurante.web.dto.table.SeatWaitlistResponse;
import com.restaurante.web.dto.table.TableResponse;
import com.restaurante.web.dto.waitlist.WaitlistSuggestionResponse;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de operación para la visualización y consulta del estado de las mesas del salón.
 */
@RestController
@RequestMapping("/operacion/mesas")
@Tag(
        name = "Mesas (Operación)",
        description = "Consulta operativa del estado actual de las mesas del salón y asignación para meseros"
)
@SecurityRequirement(name = "bearerAuth")
public class TableOperationController {

    private final TableService tableService;
    private final TableSeatingService tableSeatingService;
    private final AccountService accountService;
    private final SubaccountService subaccountService;

    public TableOperationController(
            TableService tableService,
            TableSeatingService tableSeatingService,
            AccountService accountService,
            SubaccountService subaccountService) {
        this.tableService = tableService;
        this.tableSeatingService = tableSeatingService;
        this.accountService = accountService;
        this.subaccountService = subaccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar el estado de las mesas del salón",
            description = "Permite al mesero visualizar el estado actual de cada mesa (LIBRE, RESERVADA, OCUPADA, CUENTA_SOLICITADA) con su número, capacidad y zona asignada. Permite filtrar opcionalmente por zona o por estado de mesa."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de mesas del salón obtenido exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TableResponse.class)))
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
            )
    })
    public ResponseEntity<List<TableResponse>> getTables(
            @Parameter(description = "Identificador opcional de la zona para filtrar")
            @RequestParam(required = false) Long zonaId,
            @Parameter(description = "Estado opcional de la mesa para filtrar (LIBRE, RESERVADA, OCUPADA, CUENTA_SOLICITADA)")
            @RequestParam(required = false) TableStatus estado,
            Authentication authentication) {

        return ResponseEntity.ok(tableService.getTables(zonaId, estado, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar el detalle y estado de una mesa específica",
            description = "Retorna la información y estado actual de una mesa en particular para el mesero."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mesa obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = TableResponse.class))
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
            )
    })
    public ResponseEntity<TableResponse> getTable(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(tableService.getTable(id, authentication));
    }

    @PostMapping({ "/{id}/sentar-reserva", "/{id}/sentar" })
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Sentar cliente con reserva en la mesa",
            description = "Registra la llegada y sienta al cliente de una reserva activa en su mesa asignada, cambiando el estado de la mesa de 'RESERVADA' a 'OCUPADA' y abriendo la cuenta correspondiente. Si la mesa está reservada para otro horario o cliente distinto, la acción es bloqueada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente sentado exitosamente y mesa cambiada a ocupada",
                    content = @Content(schema = @Schema(implementation = SeatReservationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o no existe reserva activa",
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
                    description = "Mesa o reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa está reservada para otro horario/cliente, o ya se encuentra ocupada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SeatReservationResponse> seatReservation(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatReservationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(tableSeatingService.seatReservation(id, request, authentication));
    }

    @PostMapping({ "/{id}/sentar-espera", "/{id}/confirmar-espera" })
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Sentar cliente sugerido de lista de espera en la mesa",
            description = "Confirma sentar al cliente sugerido de la lista de espera cuando la mesa compatible se libera, cambiando el estado de la mesa a 'OCUPADA', retirando al cliente de la lista de espera (estado SENTADA) y abriendo la cuenta respectiva."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente de la lista de espera sentado exitosamente y mesa cambiada a ocupada",
                    content = @Content(schema = @Schema(implementation = SeatWaitlistResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
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
                    description = "Mesa o entrada de lista de espera no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Mesa ocupada, reservada por horario/cliente, capacidad insuficiente o sin cliente sugerido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SeatWaitlistResponse> seatWaitlistCustomer(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatWaitlistRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(tableSeatingService.seatWaitlistEntry(id, request, authentication));
    }

    @GetMapping("/{id}/sugerencia-espera")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar cliente sugerido de lista de espera para la mesa",
            description = "Obtiene la sugerencia activa de lista de espera para la mesa liberada o genera una sugerencia si existen clientes compatibles esperando."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sugerencia de cliente de lista de espera para la mesa",
                    content = @Content(schema = @Schema(implementation = WaitlistSuggestionResponse.class))
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
            )
    })
    public ResponseEntity<WaitlistSuggestionResponse> getTableWaitlistSuggestion(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        WaitlistSuggestionResponse suggestion = tableSeatingService.getWaitlistSuggestionForTable(id, authentication);
        if (suggestion == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(suggestion);
    }

    @PostMapping("/{id}/abrir-cuenta")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Abrir cuenta en una mesa libre",
            description = "Abre una nueva cuenta asociada a la mesa libre y al mesero autenticado, registra la hora de apertura y cambia el estado de la mesa a 'OCUPADA'. Si la mesa ya tiene una cuenta activa o se encuentra ocupada, la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta abierta exitosamente y mesa marcada como ocupada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
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
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) OpenAccountRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.openAccount(id, request, authentication));
    }

    @GetMapping("/{id}/cuenta")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar cuenta activa de la mesa",
            description = "Devuelve la cuenta activa de consumo asociada actualmente a la mesa indicada."
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
    public ResponseEntity<AccountResponse> getActiveAccount(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.getActiveAccountByTable(id, authentication));
    }

    @PostMapping("/{id}/transferir-cuenta")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Transferir cuenta de la mesa a otra mesa libre",
            description = "Transfiere la cuenta activa de la mesa origen a una mesa libre destino, conservando todas las comandas ya registradas, liberando la mesa origen y ocupando la mesa destino. Si la mesa destino no está en estado 'libre' o disponible, la acción es bloqueada."
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
                    description = "Mesa origen, cuenta activa o mesa destino no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa destino no está disponible",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<TransferAccountResponse> transferTableAccount(
            @Parameter(description = "Identificador único de la mesa origen", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TransferAccountRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.transferAccountByTable(id, request, authentication));
    }

    @PostMapping("/{id}/fusionar-cuenta")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Fusionar cuenta de la mesa con otra cuenta",
            description = "Fusiona la cuenta activa de la mesa origen con la cuenta de otra mesa destino indicada, combinando sus consumos y liberando la mesa origen. Si alguna cuenta está en proceso de cobro ('LISTA_COBRO'), la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuentas fusionadas exitosamente y mesa origen liberada",
                    content = @Content(schema = @Schema(implementation = MergeAccountsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Mesa destino requerida, idéntica o misma mesa",
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
                    description = "Mesa origen, cuenta activa o mesa destino no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya está en proceso de cobro o no está en estado abierta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<MergeAccountsResponse> mergeTableAccount(
            @Parameter(description = "Identificador único de la mesa origen", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MergeAccountsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.mergeAccountsByTable(id, request, authentication));
    }

    @PostMapping("/{id}/dividir-cuenta/personas")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Dividir cuenta de la mesa por personas",
            description = "Divide la cuenta activa de la mesa entre un número de personas indicado, generando sub-cuentas con el porcentaje y subtotal correspondiente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta dividida exitosamente por personas",
                    content = @Content(schema = @Schema(implementation = SplitAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Número de personas inválido o la mesa no tiene platillos registrados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa no encontrada o no tiene cuenta activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya tiene subcuentas facturadas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SplitAccountResponse> splitTableAccountByPeople(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SplitByPeopleRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.splitByPeopleByTable(id, request, authentication));
    }

    @PostMapping("/{id}/dividir-cuenta/items")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Dividir cuenta de la mesa por ítems específicos",
            description = "Divide la cuenta activa de la mesa asignando ítems específicos a distintas sub-cuentas. Si un platillo ya fue asignado a otra sub-cuenta, la acción es impedida."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta dividida exitosamente por ítems específicos",
                    content = @Content(schema = @Schema(implementation = SplitAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o platillo no pertenece a la cuenta de la mesa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya fue asignado a otra sub-cuenta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SplitAccountResponse> splitTableAccountByItems(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SplitByItemsRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.splitByItemsByTable(id, request, authentication));
    }

    @GetMapping("/{id}/subcuentas")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar subcuentas de la mesa activa",
            description = "Devuelve el listado de sub-cuentas generadas para la cuenta activa de la mesa indicada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de subcuentas de la mesa",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubaccountResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa no encontrada o no tiene cuenta activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<SubaccountResponse>> getTableSubaccounts(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(subaccountService.getSubaccountsByTableId(id, authentication));
    }

    @PostMapping("/{id}/solicitar-cobro")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Marcar cuenta de la mesa como lista para cobro",
            description = "Marca la cuenta activa de la mesa como lista para cobro, actualiza el estado de la mesa a 'CUENTA_SOLICITADA' y genera notificación para el cajero. Requiere al menos un platillo registrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta de la mesa marcada como lista para cobro exitosamente",
                    content = @Content(schema = @Schema(implementation = RequestBillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "La cuenta no tiene platillos registrados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa no encontrada o no tiene cuenta activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La cuenta ya se encuentra marcada como lista para cobro o no está en estado ABIERTA",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RequestBillResponse> requestTableBill(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(accountService.requestBillByTable(id, authentication));
    }
}