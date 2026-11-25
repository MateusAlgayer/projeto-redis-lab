package org.acme.infra.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.acme.domain.Promocao;
import org.acme.repository.PromocaoRepository;

/**
 * Implementação do repositório de promoções utilizando Redis como backend de
 * armazenamento.
 */
@ApplicationScoped
@Slf4j
public class RedisPromocaoService implements PromocaoRepository {

  // ValueCommands é responsável por comandos relacionados a valores (SET, GET).
  private final ValueCommands<String, Promocao> valueCommands;
  // KeyCommands é responsável por comandos relacionados a chaves (EXPIRE, TTL,
  // EXISTS, KEYS/SCAN).
  private final KeyCommands<String> keyCommands;

  @Inject
  public RedisPromocaoService(RedisDataSource redis) {
    this.valueCommands = redis.value(Promocao.class);
    this.keyCommands = redis.key();
  }

  @Override
  public boolean add(Promocao promocao) {
    if (promocao == null || promocao.getNome() == null) {
      throw new IllegalArgumentException("A promoção e seu nome não podem ser nulos");
    }

    final String key = promocaoKey(promocao.getNome());
    log.info(promocao.toString());
    // Comando SETNX: grava somente se a chave não existir, sem janela entre
    // verificar e gravar
    if (!valueCommands.setnx(key, promocao)) {
      return false;
    }

    if (promocao.getDataExpiracao() != null) {
      expirarEm(key, promocao.getDataExpiracao());
    }
    return true;
  }

  // Comando EXPIRE. O mínimo de 1 segundo impede que uma data muito próxima
  // deixe a chave sem expiração.
  private void expirarEm(String key, LocalDateTime dataExpiracao) {
    final long segundos = ChronoUnit.SECONDS.between(LocalDateTime.now(), dataExpiracao);
    keyCommands.expire(key, Math.max(1, segundos));
  }

  @Override
  public Optional<Promocao> get(String item) {
    if (item == null || item.isBlank()) {
      return Optional.empty();
    }

    try {
      // Comando GET: obtém o valor mapeado para a chave
      Promocao promocao = valueCommands.get(promocaoKey(item));
      if (promocao == null) {
        return Optional.empty();
      }
      log.info(promocao.toString());
      return Optional.of(promocao);
    } catch (RedisKeyNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public Boolean exists(String item) {
    if (item == null || item.isBlank()) {
      return false;
    }

    // Comando EXISTS: verifica se a chave existe no Redis
    return keyCommands.exists(promocaoKey(item));
  }

  @Override
  public Long tempoRestante(String item) {
    if (item == null || item.isBlank()) {
      return -2L;
    }

    try {
      // Comando TTL: retorna o tempo de vida restante em segundos (-2 = inexistente,
      // -1 = sem expiração)
      return keyCommands.ttl(promocaoKey(item));
    } catch (RedisKeyNotFoundException e) {
      return -2L;
    }
  }

  @Override
  public List<Promocao> listar() {
    // Listar chaves com padrão promocao:*
    List<String> keys = keyCommands.keys(promocaoKey("*"));
    if (keys == null || keys.isEmpty()) {
      return Collections.emptyList();
    }

    return keys.stream()
        .map(valueCommands::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Promocao> prorrogar(String item, LocalDateTime novaDataExpiracao) {
    if (item == null || item.isBlank() || novaDataExpiracao == null) {
      return Optional.empty();
    }

    Optional<Promocao> optionalPromocao = get(item);
    if (optionalPromocao.isEmpty()) {
      return Optional.empty();
    }

    Promocao promocao = optionalPromocao.get();
    promocao.setDataExpiracao(novaDataExpiracao);

    final String key = promocaoKey(item);
    valueCommands.set(key, promocao);

    expirarEm(key, novaDataExpiracao);

    return Optional.of(promocao);
  }

  private String promocaoKey(String key) {
    return "promocao:%s".formatted(key);
  }
}
