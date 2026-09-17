package com.restaurante.web.admin;

import com.restaurante.application.table.TableService;
import com.restaurante.web.dto.table.OccupancyPanelTableResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/panel-ocupacion")
@Tag(
        name = "Panel de ocupación",
        description = "Consulta del estado operativo actual de las mesas"
)
@SecurityRequirement(name = "bearerAuth")
public class OccupancyPanelAdminController {

    private final TableService tableService;

    public OccupancyPanelAdminController(
            TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @Operation(summary = "Consultar el panel de ocupación")
    public List<OccupancyPanelTableResponse> getOccupancyPanel(
            Authentication authentication) {

        return tableService.getOccupancyPanel(authentication);
    }
}