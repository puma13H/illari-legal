package com.illarilegal.pago.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PagoRequest(
        @NotNull @Positive Long idOrden,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotBlank @Pattern(regexp = "PENDIENTE|APROBADO|RECHAZADO|ANULADO",
                message = "debe ser PENDIENTE, APROBADO, RECHAZADO o ANULADO") String estado
) {}
