package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad que representa una notificacion del sistema para usuarios o roles
 */
@Entity
@Table(name = "notificaciones", schema = "restaurante")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "destinatario_id")
    private Long recipientId;

    @Column(name = "rol_destinatario_id")
    private Long recipientRoleId;

    @Column(name = "tipo", nullable = false, length = 40)
    private String type;

    @Column(name = "titulo", nullable = false, length = 150)
    private String title;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String message;

    @Column(name = "entidad", length = 80)
    private String entity;

    @Column(name = "entidad_id", length = 80)
    private String entityId;

    @Column(name = "prioridad", nullable = false, length = 10)
    private String priority;

    @Column(name = "leida", nullable = false)
    private boolean read;

    @Column(name = "leida_en")
    private Instant readAt;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long restaurantId, Long recipientId, Long recipientRoleId,
            String type, String title, String message,
            String entity, String entityId, String priority) {
        this.restaurantId = restaurantId;
        this.recipientId = recipientId;
        this.recipientRoleId = recipientRoleId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.entity = entity;
        this.entityId = entityId;
        this.priority = priority != null ? priority : "MEDIA";
        this.read = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (priority == null) {
            priority = "MEDIA";
        }
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getRecipientRoleId() {
        return recipientRoleId;
    }

    public void setRecipientRoleId(Long recipientRoleId) {
        this.recipientRoleId = recipientRoleId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
