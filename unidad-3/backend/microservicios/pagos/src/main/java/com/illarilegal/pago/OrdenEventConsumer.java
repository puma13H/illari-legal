package com.illarilegal.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illarilegal.compartido.mensajeria.EventEnvelope;
import com.illarilegal.compartido.observabilidad.Correlation;
import com.illarilegal.compartido.observabilidad.ObservationCommit;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrdenEventConsumer {
  private static final Logger logger = LoggerFactory.getLogger(OrdenEventConsumer.class);

  private final PagoService pagos;
  private final ObjectMapper objectMapper;
  private final ResultadoPagoOutbox resultados;
  private final EventoRecibidoRepository recibidos;

  public OrdenEventConsumer(
      PagoService pagos,
      ObjectMapper objectMapper,
      ResultadoPagoOutbox resultados,
      EventoRecibidoRepository recibidos) {
    this.pagos = pagos;
    this.objectMapper = objectMapper;
    this.resultados = resultados;
    this.recibidos = recibidos;
  }

  @Transactional
  @KafkaListener(topics = "illari.orden.creada", groupId = "illari-pago-ms")
  public void onOrden(String payload) {
    var json = EventEnvelope.parse(payload);
    String eventId = EventEnvelope.id(json), hash = EventEnvelope.hash(json);
    try (var scope = new Correlation(json.path("traceId").asText(null), eventId)) {
      var previousEvent = recibidos.findById(eventId);
      if (previousEvent.isPresent()) {
        previousEvent.get().verificar(hash);
        return;
      }
      final OrdenCreadaEvento evento;
      try {
        evento = objectMapper.readValue(payload, OrdenCreadaEvento.class);
      } catch (JsonProcessingException exception) {
        throw new IllegalArgumentException("No se pudo leer el evento de orden creada.", exception);
      }
      if (evento == null
          || evento.ordenId() == null
          || evento.ordenId() < 1
          || evento.idCliente() == null
          || evento.idCliente().isBlank()
          || evento.total() == null
          || evento.total().signum() <= 0
          || evento.total().stripTrailingZeros().scale() > 2
          || evento.tipoEvento() == null
          || evento.metodoPago() == null
          || evento.metodoPago().isBlank()
          || !"orden-legal-ms".equals(evento.origen())) {
        throw new IllegalArgumentException(
            "El evento de orden requiere ordenId, cliente, total y metodoPago.");
      }

      String metodo = evento.metodoPago().strip().toUpperCase(java.util.Locale.ROOT);
      switch (evento.tipoEvento()) {
        case "orden.creada" -> {
          if (evento.yapeCheckoutApi()) {
            if (!"YAPE".equals(metodo) || evento.destinoTransferencia() != null) {
              throw new IllegalArgumentException(
                  "El evento Yape no corresponde a un cobro en línea.");
            }
            pagos.iniciarYape(evento.ordenId(), evento.idCliente(), evento.total());
            logger.info("saga_pago_yape_pendiente ordenId={}", evento.ordenId());
          } else if (evento.destinoTransferencia() == null) {
            logger.info("saga_pago_evento_historico ordenId={}", evento.ordenId());
          } else {
            pagos.iniciarTransferencia(
                evento.ordenId(),
                evento.idCliente(),
                evento.total(),
                metodo,
                evento.destinoTransferencia());
            logger.info(
                "saga_pago_transferencia_pendiente ordenId={} metodo={}", evento.ordenId(), metodo);
          }
        }
        case "orden.cancelacion.solicitada" -> {
          if ("MERCADOPAGO".equals(metodo)
              || evento.destinoTransferencia() != null
              || evento.yapeCheckoutApi()) {
            throw new IllegalArgumentException(
                "La devolución se debe conciliar con la cuenta receptora.");
          }
          resultados.registrar(
              pagos.cancelarDesdeOrden(evento.ordenId(), evento.idCliente(), evento.total()));
        }
        default -> throw new IllegalArgumentException("Comando de orden desconocido.");
      }
      recibidos.saveAndFlush(new EventoRecibidoEntity(eventId, hash));
      ObservationCommit.log("kafka_processed");
    }
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record OrdenCreadaEvento(
      String tipoEvento,
      Long ordenId,
      String idCliente,
      BigDecimal total,
      String metodoPago,
      String origen,
      Instant timestamp,
      DestinoTransferencia destinoTransferencia,
      @JsonAlias("yapeCulqi") boolean yapeCheckoutApi) {}

  public record DestinoTransferencia(String titular, String celular) {}

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record ResultadoPagoEvento(
      String tipoEvento,
      Long ordenId,
      String idCliente,
      BigDecimal monto,
      String estado,
      String origen,
      Instant timestamp,
      Long pagoId,
      Long revisionPago) {}
}
