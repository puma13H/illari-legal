package com.illarilegal.compartido;

import static org.junit.jupiter.api.Assertions.*;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import com.illarilegal.compartido.seguridad.ValidacionJwt;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

class SeguridadCompartidaTest {
  private Jwt token(String issuer, String audience, String subject, Instant expiration) {
    return Jwt.withTokenValue("contenido-para-validar-claims")
        .header("alg", "RS256")
        .issuer(issuer)
        .audience(List.of(audience))
        .subject(subject)
        .issuedAt(Instant.now().minusSeconds(120))
        .expiresAt(expiration)
        .build();
  }

  @Test
  void aceptaLaIdentidadVigenteDeLaAudienciaEsperada() {
    var result =
        ValidacionJwt.validadores("https://acceso/realms/illari", "illari-api")
            .validate(
                token(
                    "https://acceso/realms/illari",
                    "illari-api",
                    "cliente",
                    Instant.now().plusSeconds(60)));
    assertFalse(result.hasErrors());
  }

  @Test
  void rechazaOtroEmisorOtraAudienciaYUnTokenVencido() {
    var validator = ValidacionJwt.validadores("https://acceso/realms/illari", "illari-api");
    assertTrue(
        validator
            .validate(token("https://otro", "illari-api", "cliente", Instant.now().plusSeconds(60)))
            .hasErrors());
    assertTrue(
        validator
            .validate(
                token(
                    "https://acceso/realms/illari",
                    "otro-api",
                    "cliente",
                    Instant.now().plusSeconds(60)))
            .hasErrors());
    assertTrue(
        validator
            .validate(
                token(
                    "https://acceso/realms/illari",
                    "illari-api",
                    "cliente",
                    Instant.now().minusSeconds(60)))
            .hasErrors());
    assertTrue(
        validator
            .validate(
                token(
                    "https://acceso/realms/illari",
                    "illari-api",
                    " ",
                    Instant.now().plusSeconds(60)))
            .hasErrors());
  }

  @Test
  void elFiltroPropagaLaCorrelacionYConservaElResultadoHttp() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/servicios");
    request.addHeader("X-Trace-ID", "solicitud-compartida");
    var response = new MockHttpServletResponse();
    new TraceIdFilter()
        .doFilter(
            request,
            response,
            (ignored, result) ->
                ((jakarta.servlet.http.HttpServletResponse) result).setStatus(403));
    assertEquals("solicitud-compartida", response.getHeader("X-Trace-ID"));
    assertEquals(403, response.getStatus());
  }
}
