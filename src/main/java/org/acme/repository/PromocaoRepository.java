package org.acme.repository;

import java.util.List;
import java.util.Optional;

import org.acme.domain.Promocao;

/**
 * PromocaoRepository
 */
public interface PromocaoRepository {
  /**
   * Grava a promoção somente se o nome ainda não estiver em uso.
   *
   * @return {@code false} se já existia uma promoção com o mesmo nome
   */
  public boolean add(Promocao promocao);

  public Optional<Promocao> get(String key);

  public Boolean exists(String key);

  public Long tempoRestante(String key);

  public List<Promocao> listar();

  public Optional<Promocao> prorrogar(String key, java.time.LocalDateTime novaDataExpiracao);
}
