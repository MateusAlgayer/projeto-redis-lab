package org.acme.characterization;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Substituto da API Magic: The Gathering cujas respostas são definidas por cada
 * fixture em blocos {@code stub-mtg}:
 *
 * <pre>
 * GET /cards/1 -> 200
 * {"card": {...}}
 * </pre>
 *
 * Use {@code -> desconectar} para derrubar a conexão sem responder.
 */
public class StubMtgProgramavel implements QuarkusTestResourceLifecycleManager {

  private static final Pattern DEFINICAO = Pattern.compile("(\\S+ \\S+) -> (\\d{3}|desconectar)");

  private record Resposta(String status, String corpo) {
  }

  private final Map<String, Resposta> respostas = new ConcurrentHashMap<>();
  private final List<String> chamadas = new ArrayList<>();
  private HttpServer servidor;

  @Override
  public Map<String, String> start() {
    try {
      servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException("Não foi possível iniciar o stub da API MTG", e);
    }
    servidor.createContext("/v1", this::atender);
    servidor.start();
    return Map.of(
        "quarkus.rest-client.mtgApiUrl.url",
        "http://localhost:%d/v1".formatted(servidor.getAddress().getPort()));
  }

  @Override
  public void stop() {
    if (servidor != null) {
      servidor.stop(0);
    }
  }

  @Override
  public void inject(TestInjector injetor) {
    injetor.injectIntoFields(this, new TestInjector.MatchesType(StubMtgProgramavel.class));
  }

  public void programar(String bloco) {
    String[] partes = bloco.split("\n", 2);
    Matcher definicao = DEFINICAO.matcher(partes[0].strip());
    if (!definicao.matches()) {
      throw new IllegalArgumentException("Definição de stub inválida: " + partes[0]);
    }
    respostas.put(definicao.group(1), new Resposta(definicao.group(2), partes.length > 1 ? partes[1] : ""));
  }

  public void limpar() {
    respostas.clear();
    retirarChamadas();
  }

  /** Devolve as chamadas recebidas desde a última retirada. */
  public synchronized List<String> retirarChamadas() {
    List<String> copia = List.copyOf(chamadas);
    chamadas.clear();
    return copia;
  }

  private void atender(HttpExchange troca) throws IOException {
    String chamada = "%s %s".formatted(troca.getRequestMethod(),
        troca.getRequestURI().getRawPath().substring("/v1".length()));
    Resposta resposta = respostas.get(chamada);
    synchronized (this) {
      chamadas.add(resposta == null ? chamada + " (sem stub)" : chamada);
    }
    if (resposta != null && resposta.status().equals("desconectar")) {
      troca.close();
      return;
    }
    int status = resposta == null ? 501 : Integer.parseInt(resposta.status());
    byte[] corpo = (resposta == null ? "" : resposta.corpo()).getBytes(StandardCharsets.UTF_8);
    troca.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    troca.sendResponseHeaders(status, corpo.length == 0 ? -1 : corpo.length);
    try (OutputStream saida = troca.getResponseBody()) {
      saida.write(corpo);
    }
  }
}
