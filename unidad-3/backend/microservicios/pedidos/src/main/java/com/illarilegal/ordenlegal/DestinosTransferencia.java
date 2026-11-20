package com.illarilegal.ordenlegal;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class DestinosTransferencia {
    private final Environment environment;

    DestinosTransferencia(Environment environment) {
        this.environment = environment;
    }

    Destino obtener(String metodo) {
        String prefijo = "PAGOS_" + metodo + "_";
        String titular = environment.getProperty(prefijo + "TITULAR", "").strip();
        String celular = environment.getProperty(prefijo + "CELULAR", "").strip();
        if (titular.isEmpty() || titular.length() > 160 || !celular.matches("9[0-9]{8}")) {
            return null;
        }
        return new Destino(titular, celular);
    }

    record Destino(String titular, String celular) {}
}
