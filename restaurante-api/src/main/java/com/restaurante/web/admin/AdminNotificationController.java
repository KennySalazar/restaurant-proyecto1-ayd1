package com.restaurante.web.admin;

import com.restaurante.application.notification.NotificationService;
import com.restaurante.web.dto.notification.NotificationCountResponse;
import com.restaurante.web.dto.notification.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la bandeja y gestión de notificaciones del
 * sistema
 */
@RestController
@RequestMapping("/admin/notifications")
@Tag(name = "Notificaciones (Administración)", description = "Bandeja y gestión de notificaciones y alertas para el administrador")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Consultar notificaciones del administrador", description = "Retorna la lista de notificaciones del sistema. Permite filtrar opcionalmente para obtener únicamente las no leídas.")
    @ApiResponse(responseCode = "200", description = "Listado de notificaciones obtenido exitosamente")
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @Parameter(description = "Si es true, retorna únicamente las notificaciones no leídas") @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.listAdminNotifications(unreadOnly));
    }

    @GetMapping("/count")
    @Operation(summary = "Conteo de notificaciones no leídas", description = "Retorna la cantidad total de notificaciones pendientes de lectura.")
    @ApiResponse(responseCode = "200", description = "Conteo obtenido exitosamente")
    public ResponseEntity<NotificationCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar notificación como leída", description = "Marca una notificación específica como leída.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída exitosamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Identificador único de la notificación", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Marca todas las notificaciones pendientes del administrador como leídas.")
    @ApiResponse(responseCode = "204", description = "Todas las notificaciones fueron marcadas como leídas")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}
