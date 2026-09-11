package com.illarilegal.ordenlegal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrdenLegalResponse(Long id, Long idCliente, List<Long> servicios,
                                 BigDecimal total, String estado, Instant fechaCreacion) {}
