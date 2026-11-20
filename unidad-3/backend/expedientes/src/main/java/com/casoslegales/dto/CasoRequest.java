package com.casoslegales.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CasoRequest(
        @NotBlank @Size(max = 180) String nombreCaso,
        @NotNull @Positive Long clienteId,
        @NotNull @Positive Long abogadoId,
        @NotBlank String estado,
        @NotNull @PastOrPresent LocalDate fechaApertura,
        @Size(max = 2000) String descripcion,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tarifa,
        LocalDate fechaAudiencia) {}
