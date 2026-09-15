package com.restaurante.web.operation;

import com.restaurante.application.table.TableService;
import com.restaurante.web.dto.table.TableResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/operacion/mesas")
@Tag(
        name = "Mesas - Operación",
        description = "Consulta operativa del estado actual de las mesas"
)
@SecurityRequirement(name = "bearerAuth")
public class TableOperationController {

    private final TableService tableService;

    public TableOperationController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasRole('WAITER')")
    @Operation(summary = "Consultar las mesas para la operación")
    public List<TableResponse> getTables(
            Authentication authentication) {

        return tableService.getTables(authentication);
    }
}