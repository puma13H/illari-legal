package com.illarilegal.pago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
class ResultadoPagoOutbox {
    private final ObjectMapper objectMapper;
    private final PagoOutboxRepository outbox;

    ResultadoPagoOutbox(ObjectMapper objectMapper, PagoOutboxRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox = outbox;
    }

    void registrar(PagoService.Pago pago) {
        // Publish the actual durable state, never a state recomputed from a replay's requested method.
        String estado = pago.estado();
        String tipoEvento = switch (estado) {
            case "VALIDADO" -> "pago.validado";
            case "FALLIDO" -> "pago.fallido";
            case "CANCELADO" -> "pago.cancelado";
            case "REEMBOLSADO" -> "pago.reembolsado";
            default -> throw new IllegalStateException("Pago sin resultado publicable.");
        };
        var resultado = new OrdenEventConsumer.ResultadoPagoEvento(
                tipoEvento, pago.idOrden(), pago.idCliente(), pago.monto(), estado,
                "pago-ms", Instant.now(), pago.id(), pago.revision());
        try {
            String eventJson = objectMapper.writeValueAsString(resultado);
            var existing = outbox.findForUpdate(pago.idOrden());
            if (existing.isEmpty()) {
                outbox.save(new PagoOutboxEntity(pago.idOrden(), eventJson));
            } else {
                var previous = objectMapper.readTree(existing.get().getPayload());
                if (!estado.equals(previous.path("estado").asText())
                        || previous.path("pagoId").asLong() != pago.id()
                        || previous.path("revisionPago").asLong() != pago.revision()) {
                    existing.get().actualizarResultado(eventJson);
                    outbox.save(existing.get());
                }
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo guardar el evento de resultado de pago", exception);
        }
    }
}

