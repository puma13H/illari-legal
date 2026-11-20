package com.casoslegales.dto;

import jakarta.validation.constraints.*;

public record RegistroRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 30) String telefono,
        @Size(max = 250) String direccion,
        @Size(max = 150) String especialidad,
        @Pattern(
                        regexp = "[a-zA-Z0-9._-]{3,50}",
                        message = "Usa entre 3 y 50 letras, números, puntos o guiones")
                String usuario,
        @Size(max = 128) String claveInicial) {}
