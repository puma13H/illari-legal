package com.casoslegales.service;

import java.util.Locale;
import java.util.Set;

/** Estados terminales históricos y los disponibles en los formularios actuales. */
public final class EstadoCaso {
    private static final Set<String> TERMINALES = Set.of("cerrado", "concluido", "finalizado", "cancelado");

    private EstadoCaso() {}

    public static boolean esActivo(String estado) {
        return estado != null && !TERMINALES.contains(estado.strip().toLowerCase(Locale.ROOT));
    }
}
