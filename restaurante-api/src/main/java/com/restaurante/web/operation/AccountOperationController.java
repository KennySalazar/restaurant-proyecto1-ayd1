package com.restaurante.web.operation;

import com.restaurante.application.account.AccountService;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
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
 * Controlador de operación para la apertura y gestión de cuentas de mesa para meseros.
 */
@RestController
@RequestMapping("/operacion/cuentas")
@Tag(
        name = "Cuentas (Operación)",
        description = "Apertura y consulta de cuentas de mesa para meseros y personal de servicio"
)
@SecurityRequirement(name = "bearerAuth")
public class AccountOperationController {

    private final AccountService accountService;

    public AccountOperationController(AccountService accountService) {
        this.accountService = accountService;
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
}
