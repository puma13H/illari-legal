package com.illarilegal.ordenlegal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoOrdenRequest(
        @NotBlank @Pattern(regexp = "CREADA|EN_PROCESO|COMPLETADA|CANCELADA",
                message = "debe ser CREADA, EN_PROCESO, COMPLETADA o CANCELADA") String estado
) {}
