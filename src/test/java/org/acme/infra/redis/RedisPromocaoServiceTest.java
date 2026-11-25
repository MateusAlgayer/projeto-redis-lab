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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.acme.domain.Promocao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

class RedisPromocaoServiceTest {

  private RedisDataSource redisDataSource;
  private ValueCommands<String, Promocao> valueCommands;
  private KeyCommands<String> keyCommands;
  private RedisPromocaoService redisPromocaoService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisDataSource = mock(RedisDataSource.class);
    valueCommands = (ValueCommands<String, Promocao>) mock(ValueCommands.class);
    keyCommands = (KeyCommands<String>) mock(KeyCommands.class);

    when(redisDataSource.value(Promocao.class)).thenReturn(valueCommands);
    when(redisDataSource.key()).thenReturn(keyCommands);

    redisPromocaoService = new RedisPromocaoService(redisDataSource);
  }

  @Test
  @DisplayName("Deve gravar promoção no Redis e definir expiração quando data for futura")
  void testAddComDataFutura() {
    LocalDateTime dataFutura = LocalDateTime.now().plusDays(5).plusMinutes(30).plusSeconds(45);
    Promocao promocao = new Promocao("carnaval", 10.0, dataFutura);
    when(valueCommands.setnx("promocao:carnaval", promocao)).thenReturn(true);

    assertTrue(redisPromocaoService.add(promocao));

    verify(valueCommands, times(1)).setnx(eq("promocao:carnaval"), eq(promocao));
    verify(keyCommands, times(1)).expire(eq("promocao:carnaval"), anyLong());
  }

  @Test
  @DisplayName("Deve gravar promoção no Redis sem expiração quando dataExpiracao for nula")
  void testAddSemExpiracao() {
    Promocao promocao = new Promocao("carnaval", 10.0, null);
    when(valueCommands.setnx("promocao:carnaval", promocao)).thenReturn(true);

    redisPromocaoService.add(promocao);

    verify(valueCommands, times(1)).setnx(eq("promocao:carnaval"), eq(promocao));
    verify(keyCommands, never()).expire(anyString(), anyLong());
  }

  @Test
  @DisplayName("Deve expirar em 1 segundo quando a data já passou no momento da gravação")
  void testAddComDataPassadaExpiraEmSeguida() {
    Promocao promocao = new Promocao("carnaval", 10.0, LocalDateTime.now().minusDays(1));
    when(valueCommands.setnx("promocao:carnaval", promocao)).thenReturn(true);

    redisPromocaoService.add(promocao);

    verify(keyCommands, times(1)).expire("promocao:carnaval", 1L);
  }

  @Test
  @DisplayName("Não deve alterar o TTL quando o nome já estiver em uso")
  void testAddNomeEmUso() {
    Promocao promocao = new Promocao("carnaval", 10.0, LocalDateTime.now().plusDays(1));
    when(valueCommands.setnx("promocao:carnaval", promocao)).thenReturn(false);

    assertFalse(redisPromocaoService.add(promocao));

    verify(keyCommands, never()).expire(anyString(), anyLong());
  }

  @Test
  @DisplayName("Deve lançar exceção ao adicionar promoção com dados nulos")
  void testAddInvalido() {
    assertThrows(IllegalArgumentException.class, () -> redisPromocaoService.add(null));
    assertThrows(IllegalArgumentException.class, () -> redisPromocaoService.add(new Promocao(null, 10.0, null)));
  }

  @Test
  @DisplayName("Deve obter promoção existente do Redis")
  void testGetExistente() {
    Promocao promo = new Promocao("carnaval", 10.0, LocalDateTime.now().plusDays(2));
    when(valueCommands.get("promocao:carnaval")).thenReturn(promo);

    Optional<Promocao> resultado = redisPromocaoService.get("carnaval");

    assertTrue(resultado.isPresent());
    assertEquals("carnaval", resultado.get().getNome());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao buscar chave inexistente no Redis")
  void testGetInexistente() {
    when(valueCommands.get("promocao:inexistente")).thenReturn(null);

    Optional<Promocao> resultado = redisPromocaoService.get("inexistente");

    assertTrue(resultado.isEmpty());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao buscar chave nula ou vazia")
  void testGetChaveInvalida() {
    assertTrue(redisPromocaoService.get(null).isEmpty());
    assertTrue(redisPromocaoService.get("   ").isEmpty());
    verify(valueCommands, never()).get(anyString());
  }

  @Test
  @DisplayName("Deve verificar existência no Redis")
  void testExists() {
    when(keyCommands.exists("promocao:carnaval")).thenReturn(true);
    when(keyCommands.exists("promocao:inexistente")).thenReturn(false);

    assertTrue(redisPromocaoService.exists("carnaval"));
    assertFalse(redisPromocaoService.exists("inexistente"));
    assertFalse(redisPromocaoService.exists(null));
    assertFalse(redisPromocaoService.exists(""));
  }

  @Test
  @DisplayName("Deve consultar tempo restante no Redis")
  void testTempoRestante() {
    when(keyCommands.ttl("promocao:carnaval")).thenReturn(7200L);
    when(keyCommands.ttl("promocao:inexistente")).thenReturn(-2L);

    assertEquals(7200L, redisPromocaoService.tempoRestante("carnaval"));
    assertEquals(-2L, redisPromocaoService.tempoRestante("inexistente"));
    assertEquals(-2L, redisPromocaoService.tempoRestante(null));
    assertEquals(-2L, redisPromocaoService.tempoRestante("   "));
  }

  @Test
  @DisplayName("Deve listar promoções do Redis corretamente")
  void testListar() {
    when(keyCommands.keys("promocao:*")).thenReturn(List.of("promocao:p1", "promocao:p2"));
    Promocao p1 = new Promocao("p1", 10.0, LocalDateTime.now());
    Promocao p2 = new Promocao("p2", 20.0, LocalDateTime.now());
    when(valueCommands.get("promocao:p1")).thenReturn(p1);
    when(valueCommands.get("promocao:p2")).thenReturn(p2);

    List<Promocao> lista = redisPromocaoService.listar();

    assertNotNull(lista);
    assertEquals(2, lista.size());
    assertEquals("p1", lista.get(0).getNome());
    assertEquals("p2", lista.get(1).getNome());
  }

  @Test
  @DisplayName("Deve retornar lista vazia se não houver chaves")
  void testListarVazio() {
    when(keyCommands.keys("promocao:*")).thenReturn(Collections.emptyList());
    assertTrue(redisPromocaoService.listar().isEmpty());

    when(keyCommands.keys("promocao:*")).thenReturn(null);
    assertTrue(redisPromocaoService.listar().isEmpty());
  }

  @Test
  @DisplayName("Deve prorrogar promoção existente atualizando entidade e TTL")
  void testProrrogarExistente() {
    LocalDateTime dataAntiga = LocalDateTime.now().plusDays(1);
    LocalDateTime novaData = LocalDateTime.now().plusDays(10);
    Promocao promo = new Promocao("carnaval", 10.0, dataAntiga);
    when(valueCommands.get("promocao:carnaval")).thenReturn(promo);

    Optional<Promocao> resultado = redisPromocaoService.prorrogar("carnaval", novaData);

    assertTrue(resultado.isPresent());
    assertEquals(novaData, resultado.get().getDataExpiracao());
    verify(valueCommands, times(1)).set(eq("promocao:carnaval"), eq(promo));
    verify(keyCommands, times(1)).expire(eq("promocao:carnaval"), anyLong());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao prorrogar promoção inexistente ou com parâmetros inválidos")
  void testProrrogarInvalido() {
    when(valueCommands.get("promocao:inexistente")).thenReturn(null);

    assertTrue(redisPromocaoService.prorrogar("inexistente", LocalDateTime.now().plusDays(5)).isEmpty());
    assertTrue(redisPromocaoService.prorrogar(null, LocalDateTime.now().plusDays(5)).isEmpty());
    assertTrue(redisPromocaoService.prorrogar("   ", LocalDateTime.now().plusDays(5)).isEmpty());
    assertTrue(redisPromocaoService.prorrogar("carnaval", null).isEmpty());
  }
}


