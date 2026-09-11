package com.illarilegal.pago.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoResponse(Long id, Long idOrden, BigDecimal monto, String estado,
                           Instant fechaCreacion, Instant fechaAprobacion) {}
