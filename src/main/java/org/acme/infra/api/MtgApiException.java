package org.acme.infra.api;

/**
 * Exceção lançada quando a API Magic: The Gathering não consegue atender a
 * requisição (timeout, rate limit, 5xx, falha de rede, resposta malformada).
 *
 * Mapeada para HTTP 503 pelo recurso de cartas.
 */
public class MtgApiException extends RuntimeException {

  public MtgApiException(String mensagem) {
    super(mensagem);
  }

  public MtgApiException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
