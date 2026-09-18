package com.restaurante.web.dto.payment;

public record CustomerResponse(
        Long clienteId,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        Long saldoPuntos,
        Integer totalVisitas
) {
}
