package com.illarilegal.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoPagoRequest(
        @NotBlank @Pattern(regexp = "PENDIENTE|APROBADO|RECHAZADO|ANULADO",
                message = "debe ser PENDIENTE, APROBADO, RECHAZADO o ANULADO") String estado
) {}
