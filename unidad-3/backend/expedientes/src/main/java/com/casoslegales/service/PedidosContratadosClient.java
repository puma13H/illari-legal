package com.casoslegales.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PedidosContratadosClient {
    private final RestClient api;
    public PedidosContratadosClient(RestClient.Builder builder,
            @Value("${ILLARI_PEDIDOS_URL:http://orden-legal-ms:8083}") String base) {
        var http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(8));
        api = builder.baseUrl(base).requestFactory(factory).build();
    }
    public JsonNode pagados(int page, String token) { return get("/api/v1/ordenes/confirmadas?pagina=" + page, token); }
    public JsonNode pedido(long id, String token) { return get("/api/v1/ordenes/" + id, token); }
    private JsonNode get(String path, String token) {
        try { return api.get().uri(path).headers(h -> h.setBearerAuth(token)).retrieve().body(JsonNode.class); }
        catch (RestClientException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo verificar el pedido. No se creó una atención."); }
    }
}
