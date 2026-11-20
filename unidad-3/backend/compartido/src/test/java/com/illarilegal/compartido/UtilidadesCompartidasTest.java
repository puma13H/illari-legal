package com.illarilegal.compartido;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illarilegal.compartido.mensajeria.EventEnvelope;
import com.illarilegal.compartido.observabilidad.Correlation;
import com.illarilegal.compartido.observabilidad.SafeObservationLayout;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class UtilidadesCompartidasTest {
  @AfterEach
  void limpiarContexto() {
    MDC.clear();
  }

  @Test
  void unTrabajoAnidadoRestauraLaCorrelacionAnterior() {
    MDC.put("traceId", "solicitud-anterior");
    MDC.put("eventId", "evento-anterior");
    try (var ignored = new Correlation("solicitud-nueva", null)) {
      assertEquals("solicitud-nueva", Correlation.current());
      assertNull(MDC.get("eventId"));
    }
    assertEquals("solicitud-anterior", Correlation.current());
    assertEquals("evento-anterior", MDC.get("eventId"));
  }

  @Test
  void elEventoConservaSuIdentificadorEnUnReintento() {
    var first = EventEnvelope.parse(EventEnvelope.enrich("{\"ordenId\":15}"));
    var retry = EventEnvelope.parse(EventEnvelope.enrich(first.toString()));
    assertEquals(EventEnvelope.id(first), EventEnvelope.id(retry));
    assertEquals(2, retry.path("schemaVersion").asInt());
    assertTrue(Correlation.valid(retry.path("traceId").asText()));
  }

  @Test
  void laHuellaIgnoraMetadatosPeroDetectaOtroImporte() {
    var original = EventEnvelope.parse("{\"ordenId\":15,\"monto\":1.24,\"traceId\":\"original\"}");
    var retry =
        EventEnvelope.parse(
            "{\"traceId\":\"nuevo\",\"monto\":1.240,\"ordenId\":15,\"schemaVersion\":2}");
    assertEquals(EventEnvelope.hash(original), EventEnvelope.hash(retry));
    retry.put("monto", 2);
    assertNotEquals(EventEnvelope.hash(original), EventEnvelope.hash(retry));
  }

  @Test
  void unIdentificadorDeEventoInvalidoSeRechaza() {
    assertThrows(
        IllegalArgumentException.class, () -> EventEnvelope.enrich("{\"eventId\":\"incorrecto\"}"));
  }

  @Test
  void losRegistrosOcultanDatosNoPermitidos() throws Exception {
    var event = new LoggingEvent();
    event.setTimeStamp(0);
    event.setLevel(Level.INFO);
    event.setMessage("token=secreto correo=persona@example.com");
    event.setMDCPropertyMap(Map.of("traceId", "solicitud-segura", "eventId", "id invalido"));
    var encoded = new SafeObservationLayout().doLayout(event);
    var json = new ObjectMapper().readTree(encoded);
    assertEquals("observation_redacted", json.path("message").asText());
    assertEquals("solicitud-segura", json.path("traceId").asText());
    assertFalse(json.has("eventId"));
    assertFalse(encoded.contains("secreto"));
    assertFalse(encoded.contains("persona@example.com"));
  }
}
