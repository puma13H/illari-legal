package com.illarilegal.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 120) String nombres,
        @NotBlank @Size(max = 120) String apellidos,
        @Email @Size(max = 160) String email,
        @Size(max = 30) String telefono,
        @Size(max = 240) String direccion,
        @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO", message = "debe ser ACTIVO o INACTIVO") String estado
) {}
