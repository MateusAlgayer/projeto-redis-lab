package org.acme.infra.redis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.acme.domain.Carta;
import org.acme.repository.CartaRepository;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementação do repositório de cartas com cache-aside no Redis.
 *
 * Segue o fluxo clássico de cache-aside: a leitura é feita primeiro no Redis
 * (GET) e apenas em caso de miss a aplicação consulta a API externa e grava o
 * resultado de volta (SET + EXPIRE).
 */
@ApplicationScoped
@Slf4j
public class RedisCartaRepository implements CartaRepository {

  // ValueCommands é responsável por comandos relacionados a valores (SET, GET).
  private final ValueCommands<String, Carta> valueCommands;
  // KeyCommands é responsável por comandos relacionados a chaves (EXPIRE, TTL,
  // EXISTS, DEL, KEYS/SCAN).
  private final KeyCommands<String> keyCommands;

  @Inject
  public RedisCartaRepository(RedisDataSource redis) {
    this.valueCommands = redis.value(Carta.class);
    this.keyCommands = redis.key();
  }

  @Override
  public Optional<Carta> get(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }

    try {
      // Comando GET: lê a carta do cache
      return Optional.ofNullable(valueCommands.get(cartaKey(id)));
    } catch (RedisKeyNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public void salvar(String id, Carta carta, long ttlSegundos) {
    if (id == null || id.isBlank() || carta == null) {
      throw new IllegalArgumentException("O identificador e a carta não podem ser nulos");
    }

    final String key = cartaKey(id);
    log.debug("Gravando carta {} no cache", key);

    // Comando SET: grava a carta serializada em JSON sob a chave usada na busca
    valueCommands.set(key, carta);

    // Comando EXPIRE: define o TTL do cache para que a entrada expire sozinha
    if (ttlSegundos > 0) {
      keyCommands.expire(key, ttlSegundos);
    }
  }

  @Override
  public Boolean existe(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }

    // Comando EXISTS: verifica se a carta já está no cache
    return keyCommands.exists(cartaKey(id));
  }

  @Override
  public Long tempoRestante(String id) {
    if (id == null || id.isBlank()) {
      return -2L;
    }

    try {
      // Comando TTL: segundos restantes até a entrada do cache expirar
      return keyCommands.ttl(cartaKey(id));
    } catch (RedisKeyNotFoundException e) {
      return -2L;
    }
  }

  @Override
  public void evict(String id) {
    if (id == null || id.isBlank()) {
      return;
    }

    // Comando DEL: invalida a entrada do cache
    keyCommands.del(cartaKey(id));
  }

  @Override
  public List<Carta> listar() {
    // Listar chaves com padrão carta:*
    List<String> keys = keyCommands.keys(cartaKey("*"));
    if (keys == null || keys.isEmpty()) {
      return Collections.emptyList();
    }

    return keys.stream()
        .map(valueCommands::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String cartaKey(String id) {
    return "carta:%s".formatted(id);
  }
}
