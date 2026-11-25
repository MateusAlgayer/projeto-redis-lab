package org.acme.characterization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Mantém o catálogo de regras e os arquivos de fixture em sincronia. */
class CatalogoDeRegrasTest {

  private static final Pattern LINK = Pattern.compile("\\]\\(([^)]+\\.approved\\.md)\\)");

  @Test
  @DisplayName("Cada regra do catálogo tem um fixture e cada fixture está no catálogo")
  void catalogoEFixturesEstaoSincronizados() throws IOException {
    String catalogo = Files.readString(Fixtures.RAIZ.resolve("README.md"), StandardCharsets.UTF_8);

    List<String> catalogados = LINK.matcher(catalogo).results().map(r -> r.group(1)).sorted().toList();
    List<String> existentes = Fixtures.aprovados().stream()
        .map(a -> Fixtures.RAIZ.relativize(a).toString().replace('\\', '/'))
        .sorted()
        .toList();

    assertEquals(existentes, catalogados);
  }
}
