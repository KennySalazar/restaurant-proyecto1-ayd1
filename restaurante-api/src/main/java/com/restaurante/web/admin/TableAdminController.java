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

import java.util.List;

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
}