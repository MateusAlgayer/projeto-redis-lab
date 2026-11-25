package org.acme.characterization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Localização dos arquivos de fixture no código-fonte. */
final class Fixtures {

  static final Path RAIZ = Path.of("src", "test", "resources", "characterization");
  static final String SUFIXO_APROVADO = ".approved.md";

  private Fixtures() {
  }

  static List<Path> aprovados() {
    try (Stream<Path> arquivos = Files.walk(RAIZ)) {
      return arquivos.filter(a -> a.toString().endsWith(SUFIXO_APROVADO)).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static Path recebidoDe(Path aprovado) {
    String nome = aprovado.getFileName().toString();
    return aprovado.resolveSibling(nome.substring(0, nome.length() - SUFIXO_APROVADO.length()) + ".received.md");
  }
}
