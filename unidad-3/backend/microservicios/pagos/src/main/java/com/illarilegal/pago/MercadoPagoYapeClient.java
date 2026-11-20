package com.illarilegal.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
class MercadoPagoYapeClient {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoYapeClient.class);
    private final RestClient api;
    private final ObjectMapper mapper;
    private final boolean habilitado;
    private final String modo;
    private final String llavePublica;
    private final long vendedorId;
    private final long minimoCentimos;
    private volatile java.time.Instant proximaConsulta = java.time.Instant.EPOCH;

    MercadoPagoYapeClient(
            RestClient.Builder builder,
            ObjectMapper mapper,
            @Value("${MERCADOPAGO_YAPE_ENABLED:false}") boolean habilitado,
            @Value("${MERCADOPAGO_YAPE_MODE:test}") String modo,
            @Value("${MERCADOPAGO_YAPE_PUBLIC_KEY:}") String llavePublica,
            @Value("${MERCADOPAGO_YAPE_ACCESS_TOKEN:}") String accessToken,
            @Value("${MERCADOPAGO_YAPE_SELLER_ID:0}") long vendedorId,
            @Value("${MERCADOPAGO_YAPE_MIN_CENTIMOS:124}") long minimoCentimos) {
        this.mapper = mapper;
        this.habilitado = habilitado;
        this.modo = modo;
        this.llavePublica = llavePublica;
        this.vendedorId = vendedorId;
        if (minimoCentimos < 1 || minimoCentimos > 200000)
            throw new IllegalStateException("Configura un mínimo de Yape válido en céntimos.");
        this.minimoCentimos = minimoCentimos;
        if (habilitado
                && (!modo.matches("test|live")
                        || vendedorId <= 0
                        || !llavePublica.matches("APP_USR-[0-9A-Fa-f-]{36}")
                        || !accessToken.startsWith("APP_USR-"))) {
            throw new IllegalStateException(
                    "Yape requiere credenciales e identificador del vendedor de Mercado Pago.");
        }
        var http =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(12));
        api =
                builder.baseUrl("https://api.mercadopago.com")
                        .requestFactory(factory)
                        .defaultHeaders(headers -> headers.setBearerAuth(accessToken))
                        .build();
    }

    void verificarHabilitado() {
        if (!habilitado) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Yape no está configurado.");
        }
    }

    void verificarMonto(java.math.BigDecimal monto) {
        if (monto.movePointRight(2).longValueExact() < minimoCentimos) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El total no alcanza el mínimo configurado de Yape. No se envió una solicitud"
                        + " de cobro.");
        }
    }

    String modo() {
        return modo;
    }
    boolean permiteConsulta() { return habilitado && !java.time.Instant.now().isBefore(proximaConsulta); }

    java.util.Optional<String> buscarOrden(String referencia, java.time.Instant inicio) {
        if (!permiteConsulta()) throw resultadoDesconocido();
        var ahora = java.time.Instant.now();
        var desde = inicio.minusSeconds(300);
        // The search endpoint admits at most thirty days per query.
        if (desde.isBefore(ahora.minus(java.time.Duration.ofDays(29)))) desde = ahora.minus(java.time.Duration.ofDays(29));
        if (desde.isAfter(ahora)) desde = ahora.minusSeconds(300);
        final String begin = desde.toString(), end = ahora.toString();
        try {
            JsonNode body = api.get().uri(builder -> builder.path("/v1/orders")
                    .queryParam("begin_date", begin).queryParam("end_date", end)
                    .queryParam("external_reference", referencia).queryParam("page_size", 2).build())
                    .retrieve().body(JsonNode.class);
            if (body == null || !body.path("data").isArray()) throw resultadoDesconocido();
            JsonNode data = body.path("data");
            if (data.size() > 1 || body.path("paging").path("total").asLong(data.size()) != data.size())
                throw resultadoDesconocido();
            if (data.isEmpty()) return java.util.Optional.empty();
            JsonNode order = data.get(0);
            if (!referencia.equals(order.path("external_reference").asText()) || !esOrdenDelEntorno(order.path("id").asText()))
                throw resultadoDesconocido();
            return java.util.Optional.of(order.path("id").asText());
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 429) {
                String retry = error.getResponseHeaders() == null ? null : error.getResponseHeaders().getFirst("Retry-After");
                java.time.Instant next = ahora.plusSeconds(60);
                if (retry != null) {
                    try { next = ahora.plusSeconds(Math.max(1, Long.parseLong(retry))); }
                    catch (RuntimeException ignored) {
                        try { next = java.time.ZonedDateTime.parse(retry, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
                        catch (RuntimeException invalid) { /* Keep the conservative default. */ }
                    }
                }
                proximaConsulta = next.isAfter(ahora) ? next : ahora.plusSeconds(1);
            }
            throw resultadoDesconocido();
        } catch (RestClientException error) { throw resultadoDesconocido(); }
    }

    String llavePublica() {
        return llavePublica;
    }

    boolean esPrueba() {
        return "test".equals(modo);
    }

    boolean esOrdenDelEntorno(String id) {
        return id != null
                && id.matches("ORD[A-Z0-9]{10,61}")
                && esPrueba() == id.startsWith("ORDTST");
    }

    void verificarVendedor() {
        try {
            JsonNode vendedor = api.get().uri("/users/me").retrieve().body(JsonNode.class);
            if (vendedor != null
                    && vendedor.path("id").asLong() == vendedorId
                    && "MPE".equals(vendedor.path("site_id").asText())
                    && vendedor.path("tags").isArray()
                    && esPrueba() == tieneEtiquetaPrueba(vendedor.path("tags"))) {
                return;
            }
        } catch (RestClientException ex) {
            throw vendedorNoVerificado();
        }
        throw vendedorNoVerificado();
    }

    JsonNode cobrar(String token, String email, String celular, String monto, String referencia) {
        var metodo = Map.of("id", "yape", "type", "debit_card", "token", token);
        var pagador = Map.of("email", email, "phone", Map.of("area_code", "51", "number", celular));
        try {
            return api.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Idempotency-Key", referencia)
                    .body(
                            Map.of(
                                    "type",
                                    "online",
                                    "processing_mode",
                                    "automatic",
                                    "external_reference",
                                    referencia,
                                    "total_amount",
                                    monto,
                                    "payer",
                                    pagador,
                                    "transactions",
                                    Map.of(
                                            "payments",
                                            List.of(
                                                    Map.of(
                                                            "amount",
                                                            monto,
                                                            "payment_method",
                                                            metodo)))))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (esRechazoDeImporte(ex)) {
                String requestId = referenciaSegura(ex);
                log.warn(
                        "yape_solicitud_rechazada http=400 codigo=invalid_transaction_amount"
                                + " requestId={}",
                        requestId);
                throw new ImporteRechazado(requestId);
            }
            if (ex.getStatusCode().value() == 402 && errorDeRechazo(ex)) {
                throw new CargoDenegado();
            }
            log.warn(
                    "yape_resultado_no_confirmado http={} requestId={}",
                    ex.getStatusCode().value(),
                    referenciaSegura(ex));
            throw resultadoDesconocido();
        } catch (RestClientException ex) {
            log.warn("yape_resultado_no_confirmado tipo={}", ex.getClass().getSimpleName());
            throw resultadoDesconocido();
        }
    }

    JsonNode consultar(String ordenId) {
        try {
            return api.get().uri("/v1/orders/{id}", ordenId).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Mercado Pago no encontró esa orden.");
            }
            throw resultadoDesconocido();
        } catch (RestClientException ex) {
            throw resultadoDesconocido();
        }
    }

    private boolean errorDeRechazo(RestClientResponseException ex) {
        try {
            JsonNode error = mapper.readTree(ex.getResponseBodyAsByteArray());
            JsonNode errores = error == null ? null : error.path("errors");
            return errores != null
                    && errores.isArray()
                    && errores.size() == 1
                    && "failed".equals(errores.get(0).path("code").asText());
        } catch (IOException exJson) {
            return false;
        }
    }

    private boolean esRechazoDeImporte(RestClientResponseException ex) {
        if (ex.getStatusCode().value() != 400) return false;
        try {
            JsonNode body = mapper.readTree(ex.getResponseBodyAsByteArray());
            JsonNode errors = body == null ? null : body.path("errors");
            return body != null
                    && !body.hasNonNull("id")
                    && errors != null
                    && errors.isArray()
                    && errors.size() == 1
                    && "invalid_transaction_amount".equals(errors.get(0).path("code").asText());
        } catch (IOException exJson) {
            return false;
        }
    }

    private String referenciaSegura(RestClientResponseException ex) {
        String value =
                ex.getResponseHeaders() == null
                        ? null
                        : ex.getResponseHeaders().getFirst("x-request-id");
        return value != null && value.matches("[a-zA-Z0-9-]{1,64}") ? value : null;
    }

    private static boolean tieneEtiquetaPrueba(JsonNode etiquetas) {
        for (JsonNode etiqueta : etiquetas) {
            if ("test_user".equals(etiqueta.asText())) {
                return true;
            }
        }
        return false;
    }

    private static ResponseStatusException vendedorNoVerificado() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo verificar el vendedor de Mercado Pago para el entorno configurado.");
    }

    static ResponseStatusException resultadoDesconocido() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo confirmar el cargo de Yape. Revisa el intento antes de volver a"
                        + " cobrar.");
    }

    static class CargoDenegado extends RuntimeException {}

    static class ImporteRechazado extends RuntimeException {
        private final String requestId;

        ImporteRechazado(String requestId) {
            super("El proveedor rechazó el importe de la solicitud.");
            this.requestId = requestId;
        }

        String requestId() {
            return requestId;
        }
    }
}
