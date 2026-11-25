package org.acme.domain;

/** Os dados informados não atendem às regras de uma promoção. */
public class PromocaoInvalidaException extends RuntimeException {

  public PromocaoInvalidaException(String mensagem) {
    super(mensagem);
  }
}
