package org.acme.infra.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.acme.domain.Carta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

class RedisCartaRepositoryTest {

  private RedisDataSource redisDataSource;
  private ValueCommands<String, Carta> valueCommands;
  private KeyCommands<String> keyCommands;
  private RedisCartaRepository redisCartaRepository;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisDataSource = mock(RedisDataSource.class);
    valueCommands = (ValueCommands<String, Carta>) mock(ValueCommands.class);
    keyCommands = (KeyCommands<String>) mock(KeyCommands.class);

    when(redisDataSource.value(Carta.class)).thenReturn(valueCommands);
    when(redisDataSource.key()).thenReturn(keyCommands);

    redisCartaRepository = new RedisCartaRepository(redisDataSource);
  }

  private Carta cartaExemplo() {
    return Carta.builder().id("15e45fe0").name("Narset, Enlightened Master").build();
  }

  @Test
  @DisplayName("Deve gravar a carta no Redis e definir o TTL informado")
  void testSalvarComTtl() {
    Carta carta = cartaExemplo();

    redisCartaRepository.salvar("386616", carta, 3600L);

    verify(valueCommands, times(1)).set(eq("carta:386616"), eq(carta));
    verify(keyCommands, times(1)).expire(eq("carta:386616"), eq(3600L));
  }

  @Test
  @DisplayName("Deve gravar a carta sem expiração quando o TTL não for positivo")
  void testSalvarSemTtl() {
    redisCartaRepository.salvar("386616", cartaExemplo(), 0L);

    verify(valueCommands, times(1)).set(eq("carta:386616"), any(Carta.class));
    verify(keyCommands, never()).expire(anyString(), anyLong());
  }

  @Test
  @DisplayName("Deve lançar exceção ao salvar com dados inválidos")
  void testSalvarInvalido() {
    assertThrows(IllegalArgumentException.class, () -> redisCartaRepository.salvar(null, cartaExemplo(), 60L));
    assertThrows(IllegalArgumentException.class, () -> redisCartaRepository.salvar("  ", cartaExemplo(), 60L));
    assertThrows(IllegalArgumentException.class, () -> redisCartaRepository.salvar("386616", null, 60L));
    verify(valueCommands, never()).set(anyString(), any(Carta.class));
  }

  @Test
  @DisplayName("Deve recuperar a carta existente do cache")
  void testGetExistente() {
    when(valueCommands.get("carta:386616")).thenReturn(cartaExemplo());

    Optional<Carta> resultado = redisCartaRepository.get("386616");

    assertTrue(resultado.isPresent());
    assertEquals("Narset, Enlightened Master", resultado.get().getName());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio quando a chave não existir no cache")
  void testGetInexistente() {
    when(valueCommands.get("carta:inexistente")).thenReturn(null);

    assertTrue(redisCartaRepository.get("inexistente").isEmpty());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao buscar com identificador nulo ou vazio")
  void testGetInvalido() {
    assertTrue(redisCartaRepository.get(null).isEmpty());
    assertTrue(redisCartaRepository.get("   ").isEmpty());
    verify(valueCommands, never()).get(anyString());
  }

  @Test
  @DisplayName("Deve verificar existência da carta no cache")
  void testExiste() {
    when(keyCommands.exists("carta:386616")).thenReturn(true);
    when(keyCommands.exists("carta:inexistente")).thenReturn(false);

    assertTrue(redisCartaRepository.existe("386616"));
    assertFalse(redisCartaRepository.existe("inexistente"));
    assertFalse(redisCartaRepository.existe(null));
    assertFalse(redisCartaRepository.existe(""));
  }

  @Test
  @DisplayName("Deve consultar o TTL restante da carta no cache")
  void testTempoRestante() {
    when(keyCommands.ttl("carta:386616")).thenReturn(1800L);
    when(keyCommands.ttl("carta:inexistente")).thenReturn(-2L);

    assertEquals(1800L, redisCartaRepository.tempoRestante("386616"));
    assertEquals(-2L, redisCartaRepository.tempoRestante("inexistente"));
    assertEquals(-2L, redisCartaRepository.tempoRestante(null));
    assertEquals(-2L, redisCartaRepository.tempoRestante("   "));
  }

  @Test
  @DisplayName("Deve remover a carta do cache com DEL")
  void testEvict() {
    redisCartaRepository.evict("386616");

    verify(keyCommands, times(1)).del("carta:386616");
  }

  @Test
  @DisplayName("Deve ignorar evict com identificador inválido")
  void testEvictInvalido() {
    redisCartaRepository.evict(null);
    redisCartaRepository.evict("   ");

    verifyNoInteractions(keyCommands);
  }

  @Test
  @DisplayName("Deve listar as cartas presentes no cache")
  void testListar() {
    when(keyCommands.keys("carta:*")).thenReturn(List.of("carta:p1", "carta:p2"));
    when(valueCommands.get("carta:p1")).thenReturn(Carta.builder().id("p1").name("Card One").build());
    when(valueCommands.get("carta:p2")).thenReturn(Carta.builder().id("p2").name("Card Two").build());

    List<Carta> lista = redisCartaRepository.listar();

    assertNotNull(lista);
    assertEquals(2, lista.size());
    assertEquals("Card One", lista.get(0).getName());
    assertEquals("Card Two", lista.get(1).getName());
  }

  @Test
  @DisplayName("Deve retornar lista vazia quando não houver chaves de carta")
  void testListarVazio() {
    when(keyCommands.keys("carta:*")).thenReturn(Collections.emptyList());
    assertTrue(redisCartaRepository.listar().isEmpty());

    when(keyCommands.keys("carta:*")).thenReturn(null);
    assertTrue(redisCartaRepository.listar().isEmpty());
  }
}
