package org.acme.characterization;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

/**
 * Testes de caracterização no padrão approved fixtures: um teste para cada
 * arquivo {@code *.approved.md}. Quando o documento gerado difere do aprovado,
 * o runner grava um {@code *.received.md} ao lado e o teste falha.
 *
 * Este runner nunca altera um arquivo aprovado.
 */
@QuarkusTest
@QuarkusTestResource(value = StubMtgProgramavel.class, restrictToAnnotatedClass = true)
class CaracterizacaoTest {

  /** Prefixos das chaves gravadas pela aplicação. */
  private static final List<String> PREFIXOS = List.of("carta:*", "promocao:*");

  @Inject
  RedisDataSource redis;

  StubMtgProgramavel stub;

  @TestFactory
  Stream<DynamicTest> cenarios() {
    return Fixtures.aprovados().stream()
        .map(arquivo -> DynamicTest.dynamicTest(
            Fixtures.RAIZ.relativize(arquivo).toString(), () -> verificar(arquivo)));
  }

  private void verificar(Path aprovado) throws IOException {
    Path recebido = Fixtures.recebidoDe(aprovado);
    Files.deleteIfExists(recebido);
    limparRedis();
    stub.limpar();

    DatasRelativas datas = new DatasRelativas();
    String esperado = Files.readString(aprovado, StandardCharsets.UTF_8);
    String obtido = Fixture.renderizar(esperado, passo -> executar(passo, datas));

    if (!obtido.equals(esperado)) {
      Files.writeString(recebido, obtido, StandardCharsets.UTF_8);
      fail("Comportamento difere do aprovado. Compare:\n  git diff --no-index %s %s"
          .formatted(aprovado, recebido));
    }
  }

  private String executar(Fixture.Passo passo, DatasRelativas datas) {
    return switch (passo.tipo()) {
      case "stub-mtg" -> {
        stub.programar(passo.conteudo());
        yield null;
      }
      case "esperar" -> {
        esperar(Duration.parse("PT" + passo.conteudo().strip().toUpperCase()));
        yield null;
      }
      case "preparar" -> {
        preparar(passo.conteudo(), datas);
        yield null;
      }
      default -> verificarPasso(passo, datas);
    };
  }

  /** Requisição que só monta o cenário. Precisa ter sucesso e não gera saída. */
  private void preparar(String bloco, DatasRelativas datas) {
    int status = enviar(bloco, datas).resposta().statusCode();
    stub.retirarChamadas();
    if (status < 200 || status >= 300) {
      throw new IllegalStateException("A preparação falhou com status %d:%n%s".formatted(status, bloco));
    }
  }

  private record Troca(String caminho, Response resposta) {
  }

  private Troca enviar(String bloco, DatasRelativas datas) {
    String[] partes = datas.resolver(bloco).split("\n", 2);
    String[] linha = partes[0].strip().split(" ", 2);
    String metodo = linha[0];
    String caminho = linha[1];

    RequestSpecification requisicao = given().urlEncodingEnabled(false);
    if (partes.length > 1) {
      requisicao.contentType("application/json").body(partes[1].getBytes(StandardCharsets.UTF_8));
    }
    return new Troca(caminho, requisicao.when().request(metodo, caminho));
  }

  /** O status sempre é registrado; o restante, somente quando o passo pede. */
  private String verificarPasso(Fixture.Passo passo, DatasRelativas datas) {
    Troca troca = enviar(passo.conteudo(), datas);
    List<String> chamadas = stub.retirarChamadas();
    List<String> saida = new ArrayList<>();
    saida.add(String.valueOf(troca.resposta().statusCode()));
    for (String opcao : passo.opcoes()) {
      saida.addAll(observar(opcao, troca, chamadas));
    }
    return datas.restaurar(String.join("\n", saida));
  }

  private List<String> observar(String opcao, Troca troca, List<String> chamadas) {
    String[] partes = opcao.split(":", 2);
    List<String> campos = partes.length > 1 ? List.of(partes[1].split(",")) : List.of();
    return switch (partes[0]) {
      case "X-Cache" -> List.of("X-Cache: " + cabecalho(troca, "X-Cache"));
      case "Location" -> {
        String location = cabecalho(troca, "Location");
        yield List.of("Location: " + (location.startsWith("http")
            ? URI.create(location).getRawPath()
            : location));
      }
      case "corpo" -> {
        String corpo = new String(troca.resposta().asByteArray(), StandardCharsets.UTF_8);
        if (corpo.isBlank()) {
          yield List.of("corpo: (vazio)");
        }
        yield campos.isEmpty()
            ? List.of(Normalizador.corpo(troca.caminho(), corpo))
            : Normalizador.camposDoCorpo(troca.caminho(), corpo, campos);
      }
      case "chamadas" -> List.of("chamadas à API: "
          + (chamadas.isEmpty() ? "(nenhuma)" : String.join(", ", chamadas)));
      case "ttl" -> estadoDoRedis((chave, estado) -> {
      });
      case "redis" -> estadoDoRedis((chave, estado) -> {
        String valor = redis.value(String.class).get(chave);
        if (campos.isEmpty()) {
          estado.add("  " + Normalizador.linha(valor));
        } else {
          Normalizador.camposDoJson(valor, campos).forEach(linha -> estado.add("  " + linha));
        }
      });
      default -> throw new IllegalArgumentException("Opção desconhecida no bloco http: " + opcao);
    };
  }

  private String cabecalho(Troca troca, String nome) {
    String valor = troca.resposta().header(nome);
    return valor == null ? "(ausente)" : valor;
  }

  private List<String> chaves() {
    return PREFIXOS.stream()
        .flatMap(prefixo -> redis.key().keys(prefixo).stream())
        .sorted()
        .toList();
  }

  /** Uma linha por chave, com o TTL, seguida do que o detalhe acrescentar. */
  private List<String> estadoDoRedis(BiConsumer<String, List<String>> detalhe) {
    List<String> estado = new ArrayList<>();
    for (String chave : chaves()) {
      estado.add("%s  ttl=%s".formatted(chave, Normalizador.ttl(redis.key().ttl(chave))));
      detalhe.accept(chave, estado);
    }
    return estado.isEmpty() ? List.of("redis: (vazio)") : estado;
  }

  /** Remove apenas as chaves da aplicação, sem usar FLUSHDB. */
  private void limparRedis() {
    chaves().forEach(chave -> redis.key().del(chave));
  }

  private void esperar(Duration duracao) {
    try {
      Thread.sleep(duracao);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new UncheckedIOException(new IOException("Espera interrompida", e));
    }
  }
}
