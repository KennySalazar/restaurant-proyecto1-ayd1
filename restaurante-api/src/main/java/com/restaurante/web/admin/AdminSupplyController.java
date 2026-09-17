package com.restaurante.web.admin;

import com.restaurante.application.inventory.InventoryAdjustmentService;
import com.restaurante.application.inventory.KardexService;
import com.restaurante.application.supply.SupplyService;
import com.restaurante.web.dto.inventory.CreateInventoryAdjustmentRequest;
import com.restaurante.web.dto.inventory.InventoryAdjustmentResponse;
import com.restaurante.web.dto.kardex.KardexRecordResponse;
import com.restaurante.web.dto.supply.ConfigureStockLimitsRequest;
import com.restaurante.web.dto.supply.CreateSupplyEntryRequest;
import com.restaurante.web.dto.supply.CreateSupplyRequest;
import com.restaurante.web.dto.supply.CreateSupplyWasteRequest;
import com.restaurante.web.dto.supply.MeasurementUnitResponse;
import com.restaurante.web.dto.supply.SingleSupplyAlertStatusResponse;
import com.restaurante.web.dto.supply.SupplyAlertResponse;
import com.restaurante.web.dto.supply.SupplyAlertSummaryResponse;
import com.restaurante.web.dto.supply.SupplyCategoryResponse;
import com.restaurante.web.dto.supply.SupplyEntryRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyEntryResponse;
import com.restaurante.web.dto.supply.SupplyRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyResponse;
import com.restaurante.web.dto.supply.SupplyStockLimitsResponse;
import com.restaurante.web.dto.supply.SupplyUpdateResponse;
import com.restaurante.web.dto.supply.SupplyWasteRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyWasteReportItemResponse;
import com.restaurante.web.dto.supply.SupplyWasteResponse;
import com.restaurante.web.dto.supply.UpdateSupplyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
        private final KardexService kardexService;
        private final InventoryAdjustmentService inventoryAdjustmentService;

        public AdminSupplyController(SupplyService supplyService,
                                     KardexService kardexService,
                                     InventoryAdjustmentService inventoryAdjustmentService) {
                this.supplyService = supplyService;
                this.kardexService = kardexService;
                this.inventoryAdjustmentService = inventoryAdjustmentService;
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
        @Operation(summary = "Consultar catálogo de insumos", description = "Retorna el catálogo de insumos registrados con su nombre, unidad de medida, categoría, costo unitario y cantidad disponible en inventario. Permite filtrar opcionalmente por categoría o término de búsqueda.")
        @ApiResponse(responseCode = "200", description = "Catálogo de insumos obtenido exitosamente (devuelve lista vacía si no hay insumos)")
        public ResponseEntity<List<SupplyResponse>> listSupplies(
                        @Parameter(description = "Identificador de la categoría para filtrar (opcional)") @RequestParam(required = false) Long categoryId,
                        @Parameter(description = "Término de búsqueda por nombre o código (opcional)") @RequestParam(required = false) String search) {
                return ResponseEntity.ok(supplyService.listSupplies(categoryId, search));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Consultar detalle de un insumo por ID", description = "Retorna la información detallada de un insumo registrado en el catálogo.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Insumo encontrado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyResponse> getSupplyById(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(supplyService.getSupplyById(id));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Actualizar información de un insumo", description = "Actualiza los datos de identificación, clasificación y costo de compra de un insumo registrado en el catálogo.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Insumo actualizado exitosamente", content = @Content(schema = @Schema(implementation = SupplyUpdateResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "Nombre o código de insumo duplicado en otro registro", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyUpdateResponse> updateSupply(
                        @Parameter(description = "Identificador único del insumo a modificar", required = true) @PathVariable Long id,
                        @Valid @RequestBody UpdateSupplyRequest request) {
                return ResponseEntity.ok(supplyService.updateSupply(id, request));
        }

        @PutMapping("/{id}/stock-limits")
        @Operation(summary = "Configurar límites de stock de un insumo", description = "Configura los límites de stock mínimo y máximo de un insumo registrado para controlar existencias y detectar oportunamente la necesidad de reabastecimiento. Permite configurar únicamente el stock mínimo dejando el stock máximo sin configurar (vacío).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Límites de stock guardados exitosamente", content = @Content(schema = @Schema(implementation = SupplyStockLimitsResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Límites de stock inválidos o negativos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyStockLimitsResponse> configureStockLimits(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id,
                        @Valid @RequestBody ConfigureStockLimitsRequest request) {
                return ResponseEntity.ok(supplyService.configureStockLimits(id, request));
        }

        @GetMapping("/alerts")
        @Operation(summary = "Consultar alertas de inventario bajo", description = "Retorna la lista de alertas activas para insumos cuya cantidad disponible ha alcanzado o se encuentra por debajo de su stock mínimo. Permite filtrar opcionalmente por categoría o severidad (BAJO, AGOTADO).")
        @ApiResponse(responseCode = "200", description = "Listado de alertas obtenido exitosamente")
        public ResponseEntity<List<SupplyAlertResponse>> listLowStockAlerts(
                        @Parameter(description = "Identificador de la categoría para filtrar (opcional)") @RequestParam(required = false) Long categoryId,
                        @Parameter(description = "Nivel de severidad para filtrar: BAJO o AGOTADO (opcional)") @RequestParam(required = false) String level) {
                return ResponseEntity.ok(supplyService.listLowStockAlerts(categoryId, level));
        }

        @GetMapping("/alerts/summary")
        @Operation(summary = "Resumen de alertas de inventario bajo", description = "Retorna el resumen cuantitativo de alertas (total, agotados, bajo stock) y el listado de insumos afectados.")
        @ApiResponse(responseCode = "200", description = "Resumen de alertas obtenido exitosamente")
        public ResponseEntity<SupplyAlertSummaryResponse> getAlertSummary() {
                return ResponseEntity.ok(supplyService.getLowStockAlertSummary());
        }

        @GetMapping("/{id}/alert")
        @Operation(summary = "Consultar estado de alerta de un insumo", description = "Verifica si un insumo específico tiene una alerta activa de inventario bajo y retorna su detalle.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Estado de alerta consultado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SingleSupplyAlertStatusResponse> getSupplyAlertStatus(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(supplyService.getSupplyAlertStatus(id));
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

        @PostMapping("/{id}/entries")
        @Operation(summary = "Registrar entrada de inventario para un insumo", description = "Registra una entrada de inventario para el insumo indicado, aumentando su cantidad disponible en inventario y recalculando su costo unitario promedio ponderado.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Entrada de inventario registrada exitosamente", content = @Content(schema = @Schema(implementation = SupplyEntryRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o valores inválidos (cantidad menor o igual a cero, costo negativo, fecha futura, etc.)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyEntryRegistrationResponse> registerSupplyEntry(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id,
                        @Valid @RequestBody CreateSupplyEntryRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(supplyService.registerSupplyEntry(id, request, authentication));
        }

        @PostMapping("/entries")
        @Operation(summary = "Registrar entrada de inventario", description = "Registra una entrada de inventario indicando el identificador del insumo en el cuerpo de la solicitud.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Entrada de inventario registrada exitosamente", content = @Content(schema = @Schema(implementation = SupplyEntryRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o valores inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyEntryRegistrationResponse> registerGeneralSupplyEntry(
                        @Valid @RequestBody CreateSupplyEntryRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(supplyService.registerSupplyEntry(request.supplyId(), request, authentication));
        }

        @GetMapping("/{id}/entries")
        @Operation(summary = "Consultar historial de entradas de inventario de un insumo", description = "Retorna el historial cronológico de entradas y compras registradas para un insumo específico.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historial de entradas obtenido exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<List<SupplyEntryResponse>> listSupplyEntries(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(supplyService.listSupplyEntries(id));
        }

        @PostMapping({ "/{id}/wastes", "/{id}/mermas" })
        @Operation(summary = "Registrar merma de inventario para un insumo", description = "Registra la baja de un insumo por concepto de merma (vencimiento, daño, error de manejo, etc.), reduciendo sus existencias y alimentando el reporte de pérdidas por merma.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Merma de inventario registrada exitosamente", content = @Content(schema = @Schema(implementation = SupplyWasteRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos, cantidad menor o igual a cero, o cantidad excede existencias disponibles", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyWasteRegistrationResponse> registerSupplyWaste(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id,
                        @Valid @RequestBody CreateSupplyWasteRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(supplyService.registerSupplyWaste(id, request, authentication));
        }

        @PostMapping({ "/wastes", "/mermas" })
        @Operation(summary = "Registrar merma de inventario general", description = "Registra una merma especificando el insumo en el cuerpo de la solicitud.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Merma de inventario registrada exitosamente", content = @Content(schema = @Schema(implementation = SupplyWasteRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o cantidad inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<SupplyWasteRegistrationResponse> registerGeneralSupplyWaste(
                        @Valid @RequestBody CreateSupplyWasteRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(supplyService.registerSupplyWaste(request.supplyId(), request, authentication));
        }

        @GetMapping({ "/{id}/wastes", "/{id}/mermas" })
        @Operation(summary = "Consultar historial de mermas de un insumo", description = "Retorna el listado de bajas por merma registradas para un insumo específico.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historial de mermas obtenido exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<List<SupplyWasteResponse>> listSupplyWastes(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(supplyService.listSupplyWastes(id));
        }

        @GetMapping({ "/wastes", "/mermas", "/wastes/report", "/mermas/reporte" })
        @Operation(summary = "Reporte de mermas de inventario", description = "Retorna la información consolidada del reporte de mermas para el restaurante.")
        @ApiResponse(responseCode = "200", description = "Reporte de mermas obtenido exitosamente")
        public ResponseEntity<List<SupplyWasteReportItemResponse>> listWasteReport(
                        @Parameter(description = "Filtro opcional por identificador de insumo") @RequestParam(required = false) Long supplyId,
                        @Parameter(description = "Filtro opcional por fecha inicial (ISO: YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @Parameter(description = "Filtro opcional por fecha final (ISO: YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(supplyService.listWasteReport(supplyId, startDate, endDate));
        }

        @GetMapping("/{id}/kardex")
        @Operation(summary = "Consultar el kardex de un insumo", description = "Retorna el historial completo de movimientos de kardex (entradas por compra, salidas por venta, salidas por merma y ajustes manuales) correspondientes a un insumo específico.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Movimientos de kardex del insumo obtenidos exitosamente"),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<List<KardexRecordResponse>> getSupplyKardex(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id) {
                List<KardexRecordResponse> movements = kardexService.getSupplyKardex(id);
                if (movements.isEmpty()) {
                        return ResponseEntity.ok()
                                        .header("X-Message", "Todavía no existen movimientos registrados para este insumo")
                                        .body(movements);
                }
                return ResponseEntity.ok(movements);
        }

        @PostMapping({ "/{id}/adjustments", "/{id}/ajustes" })
        @Operation(summary = "Registrar ajuste manual de inventario para un insumo", description = "Registra un ajuste manual de existencias (aumento o disminución) para un insumo específico, actualizando existencias, registrando el movimiento en el kardex, verificando alertas de stock bajo y actualizando la disponibilidad de platillos.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Ajuste manual de inventario registrado exitosamente", content = @Content(schema = @Schema(implementation = InventoryAdjustmentResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Motivo faltante, cantidad menor o igual a cero, o cantidad excede existencias disponibles", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<InventoryAdjustmentResponse> registerAdjustment(
                        @Parameter(description = "Identificador único del insumo", required = true) @PathVariable Long id,
                        @Valid @RequestBody CreateInventoryAdjustmentRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(inventoryAdjustmentService.registerAdjustment(id, request, authentication));
        }

        @PostMapping({ "/adjustments", "/ajustes" })
        @Operation(summary = "Registrar ajuste manual de inventario general", description = "Registra un ajuste manual de existencias indicando el identificador del insumo en el cuerpo de la solicitud.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Ajuste manual de inventario registrado exitosamente", content = @Content(schema = @Schema(implementation = InventoryAdjustmentResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos o cantidad inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token JWT no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Insumo no encontrado o inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<InventoryAdjustmentResponse> registerGeneralAdjustment(
                        @Valid @RequestBody CreateInventoryAdjustmentRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(inventoryAdjustmentService.registerAdjustment(request.supplyId(), request, authentication));
        }
}
