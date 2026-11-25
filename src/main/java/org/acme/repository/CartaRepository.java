package org.acme.repository;

import java.util.List;
import java.util.Optional;

import org.acme.domain.Carta;

/**
 * Repositório de cartas responsável por ler e gravar o cache (padrão
 * cache-aside) mantido no Redis.
 */
public interface CartaRepository {

  /**
   * Recupera a carta do cache pelo identificador usado na busca.
   */
  Optional<Carta> get(String id);

  /**
   * Grava a carta no cache com o TTL informado em segundos.
   */
  void salvar(String id, Carta carta, long ttlSegundos);

  /**
   * Verifica se a carta já está no cache.
   */
  Boolean existe(String id);

  /**
   * Retorna o TTL restante da chave em segundos (-2 = inexistente, -1 = sem
   * expiração).
   */
  Long tempoRestante(String id);

  /**
   * Remove a carta do cache (invalidação).
   */
  void evict(String id);

  /**
   * Lista todas as cartas presentes no cache.
   */
  List<Carta> listar();
}
