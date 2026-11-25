package org.acme.characterization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fixa o formato do documento gerado, sem subir a aplicação. */
class FixtureTest {

  private static final String ENTRADA = """
      # Cenário

      ```preparar
      GET /a
      ```

      ```http X-Cache chamadas
      GET /b
      ```
      """;

  private static String saidaDe(Fixture.Passo passo) {
    return passo.tipo().equals("http") ? String.join(" ", passo.opcoes()) : null;
  }

  @Test
  @DisplayName("Só o passo verificado recebe resultado, e gerar de novo não muda o documento")
  void geracaoEIdempotente() {
    String gerado = Fixture.renderizar(ENTRADA, FixtureTest::saidaDe);

    assertEquals(ENTRADA.replace("GET /b\n```\n", "GET /b\n```\n\n```resultado\nX-Cache chamadas\n```\n"), gerado);
    assertEquals(gerado, Fixture.renderizar(gerado, FixtureTest::saidaDe));
  }

  @Test
  @DisplayName("Um resultado antigo em um passo que virou preparação é removido")
  void resultadoAntigoEmPreparacaoERemovido() {
    String antigo = ENTRADA.replace("GET /a\n```\n", "GET /a\n```\n\n```resultado\n200\n```\n");

    assertEquals(
        Fixture.renderizar(ENTRADA, FixtureTest::saidaDe),
        Fixture.renderizar(antigo, FixtureTest::saidaDe));
  }

  @Test
  @DisplayName("As palavras da linha de abertura chegam ao executor como opções")
  void opcoesSaoLidas() {
    Fixture.renderizar(ENTRADA, passo -> {
      assertEquals(
          passo.tipo().equals("http") ? List.of("X-Cache", "chamadas") : List.of(),
          passo.opcoes());
      return null;
    });
  }
}
