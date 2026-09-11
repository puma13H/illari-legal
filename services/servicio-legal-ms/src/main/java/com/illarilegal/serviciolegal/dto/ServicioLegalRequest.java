package com.illarilegal.serviciolegal.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServicioLegalRequest(
        @NotBlank @Size(max = 140) String nombre,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precio,
        @NotBlank @Size(max = 40) String categoria,
        @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO", message = "debe ser ACTIVO o INACTIVO") String estado
) {}
