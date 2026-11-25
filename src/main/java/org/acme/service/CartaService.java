package org.acme.service;

import java.util.Optional;

import org.acme.api.dto.CartaCacheDTO;
import org.acme.domain.Carta;
import org.acme.infra.api.MtgApiClient;
import org.acme.infra.api.MtgApiException;
import org.acme.repository.CartaRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Camada de serviço que implementa o padrão <b>cache-aside</b> para cartas de
 * Magic: The Gathering.
 *
 * Fluxo da consulta ({@link #buscarComCache(String)}):
 * <ol>
 * <li>lê a carta no Redis (cache);</li>
 * <li>em caso de <i>hit</i>, devolve o que está no cache;</li>
 * <li>em caso de <i>miss</i>, consulta a API magicthegathering.io;</li>
 * <li>grava o resultado da API no Redis com TTL e o devolve.</li>
 * </ol>
 */
@ApplicationScoped
@Slf4j
public class CartaService {

  private final CartaRepository cartaRepository;
  private final MtgApiClient mtgApiClient;
  private final long ttlSegundos;

  @Inject
  public CartaService(CartaRepository cartaRepository,
      @RestClient MtgApiClient mtgApiClient,
      @ConfigProperty(name = "carta.cache.ttl-segundos", defaultValue = "3600") long ttlSegundos) {
    this.cartaRepository = cartaRepository;
    this.mtgApiClient = mtgApiClient;
    this.ttlSegundos = ttlSegundos;
  }

  /**
   * Consulta a carta aplicando o padrão cache-aside.
   *
   * @return os dados da carta acompanhados dos metadados de cache, ou
   *         {@link Optional#empty()} se o identificador for inválido ou a API
   *         não encontrar a carta
   * @throws org.acme.infra.api.MtgApiException se a API externa estiver indisponível
   */
  public Optional<CartaCacheDTO> buscarComCache(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }

    // 1) Cache lookup
    Optional<Carta> cacheada = cartaRepository.get(id);
    if (cacheada.isPresent()) {
      log.debug("Cache HIT para a carta {}", id);
      return Optional.of(CartaCacheDTO.hit(cacheada.get(), cartaRepository.tempoRestante(id)));
    }

    // 2) Cache miss: vai na fonte de dados
    log.debug("Cache MISS para a carta {}", id);
    Optional<Carta> daApi = buscarNaApi(id);
    if (daApi.isEmpty()) {
      return Optional.empty();
    }

    // 3) Popula o cache (write-back) para as próximas consultas
    Carta carta = daApi.get();
    cartaRepository.salvar(id, carta, ttlSegundos);

    return Optional.of(CartaCacheDTO.miss(carta, ttlSegundos));
  }

  /**
   * Consulta a API Magic: The Gathering através do REST Client traduzindo as
   * falhas do transporte HTTP para o vocabulário do domínio:
   *
   * <ul>
   * <li>HTTP 404 → {@link Optional#empty()} (carta inexistente);</li>
   * <li>demais HTTP >= 400 → {@link MtgApiException} (→ HTTP 503);</li>
   * <li>falha de rede/timeout → {@link MtgApiException} (→ HTTP 503).</li>
   * </ul>
   */
  private Optional<Carta> buscarNaApi(String id) {
    try {
      final MtgApiClient.RespostaCarta resposta = mtgApiClient.buscarPorId(id);
      return Optional.ofNullable(resposta).map(MtgApiClient.RespostaCarta::card);
    } catch (WebApplicationException e) {
      final int status = e.getResponse() == null ? 0 : e.getResponse().getStatus();
      if (status == 404) {
        log.debug("Carta {} não encontrada na API Magic: The Gathering", id);
        return Optional.empty();
      }
      throw new MtgApiException(
          "API Magic: The Gathering respondeu %d para a carta %s".formatted(status, id), e);
    } catch (RuntimeException e) {
      throw new MtgApiException(
          "Falha de comunicação com a API Magic: The Gathering para a carta %s".formatted(id), e);
    }
  }

  /**
   * Verifica se a carta já está populada no cache.
   */
  public boolean estaNoCache(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    return Boolean.TRUE.equals(cartaRepository.existe(id));
  }

  /**
   * Retorna o TTL restante da carta no cache, ou -2 quando não há cache.
   */
  public long tempoRestanteNoCache(String id) {
    if (id == null || id.isBlank()) {
      return -2L;
    }
    return cartaRepository.tempoRestante(id);
  }

  /**
   * Invalida a entrada da carta no cache.
   *
   * @return {@code true} se existia uma entrada e foi removida
   */
  public boolean invalidar(String id) {
    if (!estaNoCache(id)) {
      return false;
    }
    cartaRepository.evict(id);
    log.debug("Cache invalidado para a carta {}", id);
    return true;
  }
}
