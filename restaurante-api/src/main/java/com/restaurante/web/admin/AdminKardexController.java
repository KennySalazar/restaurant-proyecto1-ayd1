package com.restaurante.web.admin;

import com.restaurante.application.inventory.KardexService;
import com.restaurante.web.dto.kardex.KardexRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador administrativo para la consulta del kardex y trazabilidad de movimientos de inventario.
 */
@RestController
@RequestMapping("/admin/kardex")
@Tag(name = "Kardex (Administración)", description = "Operaciones de consulta del kardex y trazabilidad histórica de inventario")
@SecurityRequirement(name = "bearerAuth")
public class AdminKardexController {

    private final KardexService kardexService;

    public AdminKardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    /**
     * Consulta el historial general de movimientos del kardex con filtros opcionales.
     */
    @GetMapping
    @Operation(
            summary = "Consultar el kardex de inventario",
            description = "Retorna el historial de movimientos de inventario (entradas por compra, salidas por venta, salidas por merma y ajustes manuales) ordenados cronológicamente por fecha descendente. Permite filtrar opcionalmente por insumo, tipo de movimiento y rango de fechas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial de movimientos de kardex obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Insumo especificado para filtrar no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<KardexRecordResponse>> listKardex(
            @Parameter(description = "Identificador del insumo para filtrar únicamente sus movimientos (opcional)")
            @RequestParam(required = false) Long supplyId,
            @Parameter(description = "Tipo de movimiento para filtrar (ENTRADA_COMPRA, SALIDA_VENTA, SALIDA_MERMA, AJUSTE, etc.)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Fecha inicial del rango de consulta (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha final del rango de consulta (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<KardexRecordResponse> movements = kardexService.listKardexMovements(supplyId, type, startDate, endDate);
        if (movements.isEmpty()) {
            return ResponseEntity.ok()
                    .header("X-Message", "Todavía no existen movimientos de inventario registrados")
                    .body(movements);
        }
        return ResponseEntity.ok(movements);
    }

    /**
     * Consulta los movimientos de kardex de un insumo específico.
     */
    @GetMapping("/supplies/{supplyId}")
    @Operation(
            summary = "Consultar el kardex de un insumo específico",
            description = "Retorna el historial completo de entradas, salidas y ajustes de inventario registrados para un insumo en específico."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Movimientos de kardex del insumo obtenidos exitosamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Insumo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<KardexRecordResponse>> getSupplyKardex(
            @Parameter(description = "Identificador único del insumo", required = true)
            @PathVariable Long supplyId
    ) {
        List<KardexRecordResponse> movements = kardexService.getSupplyKardex(supplyId);
        if (movements.isEmpty()) {
            return ResponseEntity.ok()
                    .header("X-Message", "Todavía no existen movimientos registrados para este insumo")
                    .body(movements);
        }
        return ResponseEntity.ok(movements);
    }
}
