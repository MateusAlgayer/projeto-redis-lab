package org.acme.infra.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Servidor HTTP stub que substitui a API Magic: The Gathering nos testes de
 * integração do REST Client, respondendo com o mesmo contrato JSON:
 *
 * <pre>
 * GET /v1/cards/386616    → 200 {"card": {...}}
 * GET /v1/cards/nao-existe → 404
 * GET /v1/cards/erro       → 500
 * </pre>
 */
public class StubMtgApi implements QuarkusTestResourceLifecycleManager {

  private static final String CARTA_JSON = """
      {
        "card": {
          "name": "Narset, Enlightened Master",
          "manaCost": "{3}{U}{R}{W}",
          "cmc": 6,
          "colors": ["White", "Blue", "Red"],
          "type": "Legendary Creature — Human Monk",
          "rarity": "Mythic Rare",
          "set": "KTK",
          "power": "4",
          "toughness": "4",
          "campo_desconhecido": "deve ser ignorado",
          "legalities": [{"format": "Commander", "legality": "Legal"}]
        }
      }
      """;

  private HttpServer servidor;

  @Override
  public Map<String, String> start() {
    try {
      servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException("Não foi possível iniciar o stub da API MTG", e);
    }

    servidor.createContext("/v1/cards/386616", ex -> responder(ex, 200, CARTA_JSON));
    servidor.createContext("/v1/cards/nao-existe", ex -> responder(ex, 404, "{\"status\":404}"));
    servidor.createContext("/v1/cards/erro", ex -> responder(ex, 500, "{\"status\":500}"));
    servidor.start();

    return Map.of(
        "quarkus.rest-client.mtgApiUrl.url",
        "http://localhost:%d/v1".formatted(servidor.getAddress().getPort()));
  }

  private void responder(HttpExchange exchange, int status, String corpo) throws IOException {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  @Override
  public void stop() {
    if (servidor != null) {
      servidor.stop(0);
    }
  }
}
