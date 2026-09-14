package com.restaurante.web.admin;

import com.restaurante.application.table.TableService;
import com.restaurante.web.dto.table.CreateTableRequest;
import com.restaurante.web.dto.table.TableResponse;
import com.restaurante.web.dto.table.TableZoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.restaurante.web.dto.table.UpdateTableRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/admin/mesas")
@Tag(name = "Mesas", description = "Gestión administrativa de mesas")
@SecurityRequirement(name = "bearerAuth")
public class TableAdminController {

    private final TableService tableService;

    public TableAdminController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    @Operation(summary = "Registrar una mesa")
    public ResponseEntity<TableResponse> createTable(
            @Valid @RequestBody CreateTableRequest request,
            Authentication authentication) {

        TableResponse response = tableService.createTable(request, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/zonas")
    @Operation(summary = "Consultar las zonas de mesa disponibles")
    public List<TableZoneResponse> getAvailableZones(Authentication authentication) {
        return tableService.getAvailableZones(authentication);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar la configuración de una mesa")
    public TableResponse updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableRequest request,
            Authentication authentication) {

        return tableService.updateTable(
                id,
                request,
                authentication
        );
    }

    @GetMapping
    @Operation(summary = "Consultar las mesas registradas")
    public List<TableResponse> getTables(
            Authentication authentication) {

        return tableService.getTables(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una mesa específica")
    public TableResponse getTable(
            @PathVariable Long id,
            Authentication authentication) {

        return tableService.getTable(
                id,
                authentication
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirar una mesa")
    public TableResponse retireTable(
            @PathVariable Long id,
            Authentication authentication) {

        return tableService.retireTable(
                id,
                authentication
        );
    }


}