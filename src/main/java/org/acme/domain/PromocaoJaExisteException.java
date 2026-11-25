package org.acme.domain;

/** Já existe uma promoção ativa com o mesmo nome. */
public class PromocaoJaExisteException extends RuntimeException {

  public PromocaoJaExisteException(String nome) {
    super("Já existe uma promoção com o nome %s".formatted(nome));
  }
}
