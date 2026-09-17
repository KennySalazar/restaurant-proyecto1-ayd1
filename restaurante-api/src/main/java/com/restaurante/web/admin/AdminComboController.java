package com.restaurante.web.admin;

import com.restaurante.application.combo.ComboService;
import com.restaurante.web.dto.combo.ComboRegistrationResponse;
import com.restaurante.web.dto.combo.ComboResponse;
import com.restaurante.web.dto.combo.ComboRetirementResponse;
import com.restaurante.web.dto.combo.ComboUpdateResponse;
import com.restaurante.web.dto.combo.CreateComboRequest;
import com.restaurante.web.dto.combo.UpdateComboRequest;
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
 * Controlador administrativo para la creación y gestión de combos y promociones comerciales.
 */
@RestController
@RequestMapping("/admin/combos")
@Tag(name = "Combos y Promociones (Administración)", description = "Operaciones administrativas para crear y gestionar combos y promociones comerciales del menú")
@SecurityRequirement(name = "bearerAuth")
public class AdminComboController {

    private final ComboService comboService;

    public AdminComboController(ComboService comboService) {
        this.comboService = comboService;
    }

    @PostMapping
    @Operation(
            summary = "Crear un combo o promoción",
            description = "Agrupa dos o más platillos activos en un combo o promoción con un precio especial de venta, conservando intactos los precios individuales de los platillos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Combo registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = ComboRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Menos de dos platillos, cantidad inválida, precio menor o igual a cero, o inclusión de platillo retirado",
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
                    description = "Uno o más platillos no encontrados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Nombre o código de combo duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComboRegistrationResponse> createCombo(@Valid @RequestBody CreateComboRequest request) {
        ComboRegistrationResponse response = comboService.createCombo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Consultar el catálogo de combos y promociones",
            description = "Retorna el listado de combos registrados con sus platillos incluidos, precio especial y ahorro estimado respecto a la compra individual."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de combos obtenido exitosamente"
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
    public ResponseEntity<List<ComboResponse>> listCombos(
            @Parameter(description = "Término de búsqueda por nombre o código (opcional)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar por estado activo (opcional)")
            @RequestParam(required = false) Boolean active
    ) {
        List<ComboResponse> combos = comboService.listCombos(search, active);
        if (combos.isEmpty()) {
            return ResponseEntity.ok()
                    .header("X-Message", "Todavía no existen combos registrados")
                    .body(combos);
        }
        return ResponseEntity.ok(combos);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar detalle de un combo por ID",
            description = "Retorna la información consolidada de un combo específico y el desglose de platillos incluidos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Combo encontrado exitosamente"
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
                    description = "Combo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComboResponse> getComboById(
            @Parameter(description = "Identificador único del combo", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(comboService.getComboById(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un combo o promoción",
            description = "Actualiza los datos comerciales, precio especial y la lista de platillos incluidos en un combo activo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Combo actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = ComboUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Menos de dos platillos, precio inválido o inclusión de platillo retirado",
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
                    description = "Combo o platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Combo retirado o conflicto de nombre/código duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComboUpdateResponse> updateCombo(
            @Parameter(description = "Identificador único del combo", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateComboRequest request
    ) {
        return ResponseEntity.ok(comboService.updateCombo(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Retirar un combo o promoción del menú",
            description = "Marca el combo como retirado e inactivo, impidiendo que sea seleccionado en nuevas comandas sin eliminar su información histórica de ventas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Combo retirado exitosamente",
                    content = @Content(schema = @Schema(implementation = ComboRetirementResponse.class))
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
                    description = "Combo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El combo ya se encuentra retirado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComboRetirementResponse> retireCombo(
            @Parameter(description = "Identificador único del combo", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(comboService.retireCombo(id));
    }
}
