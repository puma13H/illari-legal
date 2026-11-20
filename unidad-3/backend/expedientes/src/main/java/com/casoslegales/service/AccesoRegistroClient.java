package com.casoslegales.service;

import com.casoslegales.dto.RegistroRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Credencial de servicio limitada al realm; nunca se entrega al navegador. */
@Component
public class AccesoRegistroClient {
    private final ObjectMapper json;
    private final String base, secret, clientId;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public AccesoRegistroClient(
            ObjectMapper json,
            @Value("${ILLARI_REGISTROS_URL:http://keycloak:8080}") String base,
            @Value("${ILLARI_REGISTROS_CLIENT_ID:illari-registros}") String clientId,
            @Value("${ILLARI_REGISTROS_CLIENT_SECRET:}") String secret) {
        this.json = json;
        this.base = base.replaceAll("/$", "");
        this.clientId = clientId;
        this.secret = secret;
    }

    public String crear(RegistroRequest request, String rol) {
        Map<String, Object> body = datos(request);
        body.put("username", request.usuario());
        body.put("enabled", false);
        body.put(
                "credentials",
                List.of(
                        Map.of(
                                "type",
                                "password",
                                "value",
                                request.claveInicial(),
                                "temporary",
                                true)));
        var created = call("POST", "/users", body);
        String location =
                created.headers()
                        .firstValue("Location")
                        .orElseThrow(() -> failure("No se pudo identificar la cuenta creada."));
        String subject = location.substring(location.lastIndexOf('/') + 1);
        if (!subject.matches("[a-zA-Z0-9-]{8,80}"))
            throw failure("La cuenta no devolvió un identificador válido.");
        try {
            Object role = json.readValue(call("GET", "/roles/" + rol, null).body(), Map.class);
            call("POST", "/users/" + subject + "/role-mappings/realm", List.of(role));
            return subject;
        } catch (Exception ex) {
            eliminarCreado(subject);
            throw failure("No se pudo asignar el perfil de acceso.");
        }
    }

    public Map<String, Object> obtener(String subject) {
        try {
            return json.readValue(
                    call("GET", "/users/" + subject, null).body(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (java.io.IOException ex) {
            throw failure("No se pudo consultar la cuenta de acceso.");
        }
    }

    public void activar(String subject) {
        call("PUT", "/users/" + subject, Map.of("enabled", true));
    }

    public void actualizar(String subject, RegistroRequest request) {
        call("PUT", "/users/" + subject, datos(request));
    }

    public void restaurar(String subject, Map<String, Object> previous) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (String key : List.of("firstName", "lastName", "email", "enabled"))
            if (previous.containsKey(key)) body.put(key, previous.get(key));
        call("PUT", "/users/" + subject, body);
    }

    public void eliminarCreado(String subject) {
        call("DELETE", "/users/" + subject, null);
    }

    private Map<String, Object> datos(RegistroRequest request) {
        String[] name = request.nombre().trim().split("\\s+", 2);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", name[0]);
        body.put("lastName", name.length > 1 ? name[1] : "");
        body.put("email", request.email().trim().toLowerCase(Locale.ROOT));
        return body;
    }

    private HttpResponse<String> call(String method, String path, Object body) {
        if (secret.isBlank()) throw failure("La gestión de accesos aún no está configurada.");
        try {
            String form =
                    "grant_type=client_credentials&client_id="
                            + encode(clientId)
                            + "&client_secret="
                            + encode(secret);
            var token =
                    send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    base
                                                            + "/realms/illari-legal/protocol/openid-connect/token"))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString(form)));
            if (token.statusCode() != 200)
                throw failure("El servicio de acceso no está disponible.");
            String access = json.readTree(token.body()).path("access_token").asText();
            var builder =
                    HttpRequest.newBuilder(URI.create(base + "/admin/realms/illari-legal" + path))
                            .header("Authorization", "Bearer " + access)
                            .header("Content-Type", "application/json");
            var response =
                    send(
                            builder.method(
                                    method,
                                    body == null
                                            ? HttpRequest.BodyPublishers.noBody()
                                            : HttpRequest.BodyPublishers.ofString(
                                                    json.writeValueAsString(body))));
            if (response.statusCode() == 409)
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El usuario o correo ya está registrado.");
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw failure("No se pudo completar la operación de acceso.");
            return response;
        } catch (java.io.IOException ex) {
            throw failure("No se pudo conectar con el servicio de acceso.");
        }
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws java.io.IOException {
        try {
            return http.send(
                    builder.timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw failure("La operación de acceso fue interrumpida.");
        }
    }

    private String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private ResponseStatusException failure(String text) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, text);
    }
}
