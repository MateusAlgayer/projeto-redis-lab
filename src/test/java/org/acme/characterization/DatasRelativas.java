package org.acme.characterization;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte placeholders como {@code {{agora+1h}}} em datas concretas e faz o
 * caminho inverso na saída. O instante "agora" é fixo durante um fixture.
 */
final class DatasRelativas {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{agora([+-])(\\d+)([smhd])}}");
  private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
  private final Map<String, String> usadas = new LinkedHashMap<>();

  String resolver(String texto) {
    return PLACEHOLDER.matcher(texto).replaceAll(m -> {
      long quantidade = Long.parseLong(m.group(2)) * (m.group(1).equals("-") ? -1 : 1);
      Duration deslocamento = switch (m.group(3)) {
        case "s" -> Duration.ofSeconds(quantidade);
        case "m" -> Duration.ofMinutes(quantidade);
        case "h" -> Duration.ofHours(quantidade);
        default -> Duration.ofDays(quantidade);
      };
      String data = FORMATO.format(agora.plus(deslocamento));
      usadas.put(data, m.group());
      return Matcher.quoteReplacement(data);
    });
  }

  /** Só substitui datas que o próprio fixture informou; as demais ficam literais. */
  String restaurar(String texto) {
    String restaurado = texto;
    for (Map.Entry<String, String> entrada : usadas.entrySet()) {
      restaurado = restaurado.replace(entrada.getKey(), entrada.getValue());
    }
    return restaurado;
  }
}
