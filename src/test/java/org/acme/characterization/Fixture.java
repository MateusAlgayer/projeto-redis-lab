package org.acme.characterization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Documento Markdown de um cenário. Os blocos {@code resultado} pertencem ao
 * runner; todo o restante do arquivo é preservado como foi escrito.
 */
final class Fixture {

  /**
   * Bloco de entrada executável. As opções são as palavras que seguem o tipo na
   * abertura do bloco e dizem o que o passo valida.
   */
  record Passo(String tipo, List<String> opcoes, String conteudo) {
  }

  private static final Pattern RESULTADO = Pattern.compile("\\n\\n```resultado\\n.*?\\n```(?=\\n|$)", Pattern.DOTALL);

  private static final Pattern ENTRADA = Pattern.compile(
      "^```(http|preparar|stub-mtg|esperar)((?: +[^ \\n]+)*)\\n(.*?)\\n```(?=\\n|$)",
      Pattern.DOTALL | Pattern.MULTILINE);

  private Fixture() {
  }

  /**
   * Executa os blocos de entrada na ordem em que aparecem e devolve o documento
   * completo, com os resultados gerados novamente.
   *
   * @param documento conteúdo do arquivo aprovado
   * @param executor  executa um passo; devolve o resultado ou {@code null} quando
   *                  o passo não produz saída
   */
  static String renderizar(String documento, Function<Passo, String> executor) {
    String semResultados = RESULTADO.matcher(documento.replace("\r\n", "\n")).replaceAll("");
    Matcher entradas = ENTRADA.matcher(semResultados);
    StringBuilder saida = new StringBuilder();
    List<Passo> executados = new ArrayList<>();
    while (entradas.find()) {
      List<String> opcoes = entradas.group(2).isBlank()
          ? List.of()
          : List.of(entradas.group(2).strip().split(" +"));
      Passo passo = new Passo(entradas.group(1), opcoes, entradas.group(3));
      String resultado = executor.apply(passo);
      executados.add(passo);
      String bloco = resultado == null
          ? entradas.group()
          : entradas.group() + "\n\n```resultado\n" + resultado + "\n```";
      entradas.appendReplacement(saida, Matcher.quoteReplacement(bloco));
    }
    entradas.appendTail(saida);
    if (executados.stream().noneMatch(p -> p.tipo().equals("http"))) {
      throw new IllegalStateException("O fixture não tem nenhum bloco http");
    }
    return saida.toString();
  }
}
