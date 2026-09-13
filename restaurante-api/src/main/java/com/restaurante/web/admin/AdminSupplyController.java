package com.restaurante.web.admin;

import com.restaurante.application.supply.SupplyService;
import com.restaurante.web.dto.supply.CreateSupplyRequest;
import com.restaurante.web.dto.supply.MeasurementUnitResponse;
import com.restaurante.web.dto.supply.SupplyCategoryResponse;
import com.restaurante.web.dto.supply.SupplyRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la gestion del catalogo de insumos
 */
@RestController
@RequestMapping("/admin/supplies")
@Tag(name = "Insumos (Administración)", description = "Operaciones administrativas para el catálogo de insumos y materia prima")
@SecurityRequirement(name = "bearerAuth")
public class AdminSupplyController {

        private final SupplyService supplyService;

        public AdminSupplyController(SupplyService supplyService) {
                this.supplyService = supplyService;
        }

        @PostMapping
        @Operation(summary = "Registrar un nuevo insumo", description = "Registra un nuevo insumo con su información básica, categoría, unidad de medida y costo de compra unitario.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Insumo registrado correctamente en el catálogo", content = @Content(schema = @Schema(implementation = SupplyRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o valores inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "Nombre o código de insumo duplicado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyRegistrationResponse> registerSupply(
                        @Valid @RequestBody CreateSupplyRequest request) {
                SupplyRegistrationResponse response = supplyService.registerSupply(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping
        @Operation(summary = "Consultar catálogo de insumos [HU-002]", description = "Retorna el catálogo de insumos registrados con su nombre, unidad de medida, categoría, costo unitario y cantidad disponible en inventario. Permite filtrar opcionalmente por categoría o término de búsqueda.")
        @ApiResponse(responseCode = "200", description = "Catálogo de insumos obtenido exitosamente (devuelve lista vacía si no hay insumos)")
        public ResponseEntity<List<SupplyResponse>> listSupplies(
                        @Parameter(description = "Identificador de la categoría para filtrar (opcional)") @RequestParam(required = false) Long categoryId,
                        @Parameter(description = "Término de búsqueda por nombre o código (opcional)") @RequestParam(required = false) String search) {
                return ResponseEntity.ok(supplyService.listSupplies(categoryId, search));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Consultar detalle de un insumo por ID [HU-002]", description = "Retorna la información detallada de un insumo registrado en el catálogo.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Insumo encontrado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyResponse> getSupplyById(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(supplyService.getSupplyById(id));
        }

        @GetMapping("/categories")
        @Operation(summary = "Listar categorías de insumo", description = "Retorna las categorías activas para selección al registrar o filtrar insumos.")
        @ApiResponse(responseCode = "200", description = "Listado de categorías obtenido exitosamente")
        public ResponseEntity<List<SupplyCategoryResponse>> listCategories() {
                return ResponseEntity.ok(supplyService.listCategories());
        }

        @GetMapping("/measurement-units")
        @Operation(summary = "Listar unidades de medida", description = "Retorna las unidades de medida del sistema (ej. Gramo, Kilogramo, Litro, Unidad).")
        @ApiResponse(responseCode = "200", description = "Listado de unidades de medida obtenido exitosamente")
        public ResponseEntity<List<MeasurementUnitResponse>> listMeasurementUnits() {
                return ResponseEntity.ok(supplyService.listMeasurementUnits());
        }
}
