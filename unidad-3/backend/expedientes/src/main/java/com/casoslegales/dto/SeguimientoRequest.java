package com.casoslegales.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record SeguimientoRequest(@NotBlank String estado, LocalDate fechaAudiencia) {}
