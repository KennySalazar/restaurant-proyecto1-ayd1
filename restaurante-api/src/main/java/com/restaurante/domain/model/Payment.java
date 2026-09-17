package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagos", schema = "restaurante")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long invoiceId;

    @Column(name = "metodo_pago_id", nullable = false)
    private Short paymentMethodId;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "cambio_entregado", precision = 12, scale = 2)
    private BigDecimal changeGiven;

    @Column(name = "referencia", length = 120)
    private String reference;

    @Column(name = "autorizacion", length = 120)
    private String authorization;

    @Column(name = "registrado_por_id", nullable = false)
    private Long registeredById;

    @Column(name = "pagado_en", nullable = false)
    private Instant paidAt;

    protected Payment() {
    }

    public Payment(
            Long invoiceId,
            Short paymentMethodId,
            BigDecimal amount,
            BigDecimal receivedAmount,
            String reference,
            String authorization,
            Long registeredById) {

        this.invoiceId = invoiceId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.receivedAmount = receivedAmount;
        this.reference = normalize(reference);
        this.authorization = normalize(authorization);
        this.registeredById = registeredById;
        this.paidAt = Instant.now();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public Short getPaymentMethodId() {
        return paymentMethodId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public BigDecimal getChangeGiven() {
        return changeGiven;
    }

    public String getReference() {
        return reference;
    }

    public String getAuthorization() {
        return authorization;
    }

    public Long getRegisteredById() {
        return registeredById;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}