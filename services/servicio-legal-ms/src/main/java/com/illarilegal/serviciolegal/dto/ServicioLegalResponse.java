package com.illarilegal.serviciolegal.dto;

import java.math.BigDecimal;

public record ServicioLegalResponse(Long id, String nombre, String descripcion,
                                    BigDecimal precio, String categoria, String estado) {}
