package com.illarilegal.ordenlegal.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OrdenLegalRequest(
        @NotNull @Positive Long idCliente,
        @NotEmpty List<@Positive Long> servicios,
        @NotNull @DecimalMin(value = "0.00") BigDecimal total,
        @NotBlank @Pattern(regexp = "CREADA|EN_PROCESO|COMPLETADA|CANCELADA",
                message = "debe ser CREADA, EN_PROCESO, COMPLETADA o CANCELADA") String estado
) {}
