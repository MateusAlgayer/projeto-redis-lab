package org.acme.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Bloqueia dependencias bidirecionais entre pacotes: um pacote pode importar de
 * outro, mas nunca ser importado pelo mesmo pacote de volta.
 */
class BidirectionalDependencyTest {

  private static final String PRODUCTION_PACKAGE = "org.acme";
  private static final String FIXTURE_PACKAGE = "org.acme.architecture.fixture";

  @Test
  @DisplayName("Nenhum pacote de producao importa e e importado pelo mesmo pacote")
  void pacotesDeProducaoNaoPossuemDependenciasBidirecionais() {
    JavaClasses producao = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(PRODUCTION_PACKAGE);

    List<String> violacoes = dependenciasBidirecionais(producao);

    assertEquals(List.of(), violacoes,
        () -> "Dependencias bidirecionais encontradas:\n" + String.join("\n", violacoes));
  }

  @Test
  @DisplayName("A regra detecta um par de pacotes que se importam mutuamente")
  void regraDetectaDependenciaBidirecional() {
    JavaClasses fixture = new ClassFileImporter().importPackages(FIXTURE_PACKAGE);

    List<String> violacoes = dependenciasBidirecionais(fixture);

    assertEquals(1, violacoes.size(), () -> "Esperava exatamente 1 violacao, mas encontrou:\n"
        + String.join("\n", violacoes));
    assertTrue(violacoes.get(0).contains("fixture.alpha") && violacoes.get(0).contains("fixture.beta"),
        () -> "Violacao deveria citar alpha e beta: " + violacoes.get(0));
  }

  private static List<String> dependenciasBidirecionais(JavaClasses classes) {
    Set<String> pacotes = new TreeSet<>();
    for (JavaClass javaClass : classes) {
      pacotes.add(javaClass.getPackageName());
    }

    Map<String, Map<String, String>> exemplos = new TreeMap<>();
    for (JavaClass javaClass : classes) {
      String origem = javaClass.getPackageName();
      for (Dependency dependencia : javaClass.getDirectDependenciesFromSelf()) {
        String destino = dependencia.getTargetClass().getPackageName();
        if (!origem.equals(destino) && pacotes.contains(destino)) {
          exemplos.computeIfAbsent(origem, chave -> new TreeMap<>()).put(destino, javaClass.getName());
        }
      }
    }

    List<String> ordenados = new ArrayList<>(pacotes);
    List<String> violacoes = new ArrayList<>();
    for (int i = 0; i < ordenados.size(); i++) {
      for (int j = i + 1; j < ordenados.size(); j++) {
        String primeiro = ordenados.get(i);
        String segundo = ordenados.get(j);
        Map<String, String> saidasDoPrimeiro = exemplos.getOrDefault(primeiro, Map.of());
        Map<String, String> saidasDoSegundo = exemplos.getOrDefault(segundo, Map.of());
        if (saidasDoPrimeiro.containsKey(segundo) && saidasDoSegundo.containsKey(primeiro)) {
          violacoes.add(primeiro + " -> " + segundo + " (via " + saidasDoPrimeiro.get(segundo) + ")"
              + " e " + segundo + " -> " + primeiro + " (via " + saidasDoSegundo.get(primeiro) + ")");
        }
      }
    }
    return violacoes;
  }
}
