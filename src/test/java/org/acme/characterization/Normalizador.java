package org.acme.characterization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Remove a variação não determinística da saída. Cada regra aqui esconde alguma
 * informação, por isso elas são poucas e restritas a casos específicos.
 */
final class Normalizador {

  private static final ObjectMapper JSON = new ObjectMapper()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  private Normalizador() {
  }

  /** TTL arredondado para cima em minutos; -1 e -2 têm nomes próprios. */
  static String ttl(long segundos) {
    if (segundos == -1) {
      return "sem-expiracao";
    }
    if (segundos == -2) {
      return "inexistente";
    }
    return "~%dmin".formatted(Math.ceilDiv(segundos, 60));
  }

  /** Corpo de uma resposta HTTP em formato legível e com chaves ordenadas. */
  static String corpo(String caminho, String corpo) {
    if (corpo.isBlank()) {
      return "";
    }
    if (caminho.contains("/tempoRestante") && corpo.matches("-?\\d+")) {
      return ttl(Long.parseLong(corpo));
    }
    Object valor = ler(corpo);
    if (valor == null) {
      return corpo;
    }
    DefaultIndenter indentacao = new DefaultIndenter("  ", "\n");
    DefaultPrettyPrinter formato = new DefaultPrettyPrinter()
        .withObjectIndenter(indentacao)
        .withArrayIndenter(indentacao);
    return escrever(normalizar(caminho, valor), formato);
  }

  /** Somente os campos pedidos do corpo da resposta, um por linha. */
  static List<String> camposDoCorpo(String caminho, String corpo, List<String> campos) {
    Object valor = ler(corpo);
    return campos(valor == null ? null : normalizar(caminho, valor), campos);
  }

  /** Somente os campos pedidos de um JSON, um por linha. */
  static List<String> camposDoJson(String json, List<String> campos) {
    return campos(ler(json), campos);
  }

  private static List<String> campos(Object valor, List<String> campos) {
    return campos.stream()
        .map(campo -> "%s: %s".formatted(campo, extrair(valor, campo.split("\\."), 0)))
        .toList();
  }

  /** Segue o caminho {@code a.b}; em listas, aplica o restante a cada item. */
  private static String extrair(Object valor, String[] partes, int nivel) {
    if (valor instanceof List<?> lista && nivel < partes.length) {
      return lista.stream()
          .map(item -> extrair(item, partes, nivel))
          .collect(Collectors.joining(", ", "[", "]"));
    }
    if (nivel == partes.length) {
      return escrever(valor, null);
    }
    // Um campo ausente é diferente de um campo presente com valor nulo.
    if (valor instanceof Map<?, ?> mapa && mapa.containsKey(partes[nivel])) {
      return extrair(mapa.get(partes[nivel]), partes, nivel + 1);
    }
    return "(ausente)";
  }

  private static Object normalizar(String caminho, Object valor) {
    if (valor instanceof Map<?, ?> mapa && mapa.get("tempoRestanteCache") instanceof Number restante) {
      @SuppressWarnings("unchecked")
      Map<String, Object> objeto = (Map<String, Object>) mapa;
      objeto.put("tempoRestanteCache", ttl(restante.longValue()));
    }
    // A listagem usa KEYS, que não garante ordem.
    if (caminho.equals("/promocao/listar") && valor instanceof List<?> lista) {
      List<Object> ordenada = new ArrayList<>(lista);
      ordenada.sort(Comparator.comparing(item -> String.valueOf(((Map<?, ?>) item).get("nome"))));
      return ordenada;
    }
    return valor;
  }

  /** JSON em uma única linha com chaves ordenadas. */
  static String linha(String texto) {
    Object valor = ler(texto);
    return valor == null ? texto : escrever(valor, null);
  }

  private static Object ler(String texto) {
    try {
      return JSON.readValue(texto, Object.class);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private static String escrever(Object valor, DefaultPrettyPrinter formato) {
    try {
      return formato == null
          ? JSON.writeValueAsString(valor)
          : JSON.writer(formato).writeValueAsString(valor);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
