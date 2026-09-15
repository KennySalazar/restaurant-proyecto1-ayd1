package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "metodos_pago", schema = "restaurante")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "codigo", nullable = false, length = 20)
    private String code;

    @Column(name = "nombre", nullable = false, length = 60)
    private String name;

    @Column(name = "afecta_efectivo", nullable = false)
    private boolean affectsCash;

    @Column(name = "requiere_referencia", nullable = false)
    private boolean requiresReference;

    @Column(name = "activo", nullable = false)
    private boolean active;

    protected PaymentMethod() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isAffectsCash() {
        return affectsCash;
    }

    public boolean isRequiresReference() {
        return requiresReference;
    }

    public boolean isActive() {
        return active;
    }
}