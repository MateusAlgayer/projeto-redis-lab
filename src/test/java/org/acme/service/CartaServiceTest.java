package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.acme.api.dto.CartaCacheDTO;
import org.acme.domain.Carta;
import org.acme.infra.api.MtgApiClient;
import org.acme.infra.api.MtgApiException;
import org.acme.repository.CartaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

class CartaServiceTest {

  private static final String ID = "386616";
  private static final long TTL = 3600L;

  private CartaRepository cartaRepository;
  private MtgApiClient mtgApiClient;
  private CartaService cartaService;

  @BeforeEach
  void setUp() {
    cartaRepository = Mockito.mock(CartaRepository.class);
    mtgApiClient = Mockito.mock(MtgApiClient.class);
    cartaService = new CartaService(cartaRepository, mtgApiClient, TTL);
  }

  private Carta cartaExemplo() {
    return Carta.builder().id("15e45fe0").name("Narset, Enlightened Master").rarity("Mythic Rare").build();
  }

  private static WebApplicationException erroHttp(int status) {
    return new WebApplicationException(Response.status(status).build());
  }

  @Test
  @DisplayName("Deve retornar a carta do Redis sem chamar a API quando houver cache HIT")
  void testCacheHit() {
    Carta carta = cartaExemplo();
    when(cartaRepository.get(ID)).thenReturn(Optional.of(carta));
    when(cartaRepository.tempoRestante(ID)).thenReturn(1200L);

    Optional<CartaCacheDTO> resultado = cartaService.buscarComCache(ID);

    assertTrue(resultado.isPresent());
    assertTrue(resultado.get().isCacheHit());
    assertEquals(CartaCacheDTO.FONTE_REDIS, resultado.get().getFonte());
    assertEquals("Narset, Enlightened Master", resultado.get().getCarta().getName());
    assertEquals(1200L, resultado.get().getTempoRestanteCache());
    verify(mtgApiClient, never()).buscarPorId(anyString());
    verify(cartaRepository, never()).salvar(anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("Deve consultar a API, gravar no Redis e retornar cache MISS quando não houver cache")
  void testCacheMiss() {
    Carta carta = cartaExemplo();
    when(cartaRepository.get(ID)).thenReturn(Optional.empty());
    when(mtgApiClient.buscarPorId(ID)).thenReturn(new MtgApiClient.RespostaCarta(carta));

    Optional<CartaCacheDTO> resultado = cartaService.buscarComCache(ID);

    assertTrue(resultado.isPresent());
    assertFalse(resultado.get().isCacheHit());
    assertEquals(CartaCacheDTO.FONTE_API, resultado.get().getFonte());
    assertEquals(TTL, resultado.get().getTempoRestanteCache());
    verify(cartaRepository, times(1)).salvar(eq(ID), eq(carta), eq(TTL));
  }

  @Test
  @DisplayName("Deve retornar Optional vazio quando a API responder 404 e não gravar no cache")
  void testCartaInexistenteNaApi() {
    when(cartaRepository.get(ID)).thenReturn(Optional.empty());
    when(mtgApiClient.buscarPorId(ID)).thenThrow(erroHttp(404));

    Optional<CartaCacheDTO> resultado = cartaService.buscarComCache(ID);

    assertTrue(resultado.isEmpty());
    verify(cartaRepository, never()).salvar(anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("Deve lançar MtgApiException quando a API responder erro interno (HTTP 500)")
  void testApiRespondeErro() {
    when(cartaRepository.get(ID)).thenReturn(Optional.empty());
    when(mtgApiClient.buscarPorId(ID)).thenThrow(erroHttp(500));

    assertThrows(MtgApiException.class, () -> cartaService.buscarComCache(ID));

    verify(cartaRepository, never()).salvar(anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("Deve lançar MtgApiException quando a API estiver indisponível (falha de transporte)")
  void testApiIndisponivel() {
    when(cartaRepository.get(ID)).thenReturn(Optional.empty());
    when(mtgApiClient.buscarPorId(ID)).thenThrow(new ProcessingException("Connection refused"));

    assertThrows(MtgApiException.class, () -> cartaService.buscarComCache(ID));

    verify(cartaRepository, never()).salvar(anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao buscar com identificador nulo ou vazio sem tocar no cache")
  void testIdentificadorInvalido() {
    assertTrue(cartaService.buscarComCache(null).isEmpty());
    assertTrue(cartaService.buscarComCache("   ").isEmpty());
    verify(cartaRepository, never()).get(any());
    verify(mtgApiClient, never()).buscarPorId(any());
  }

  @Test
  @DisplayName("Deve verificar se a carta está no cache")
  void testEstaNoCache() {
    when(cartaRepository.existe(ID)).thenReturn(true);
    when(cartaRepository.existe("outro")).thenReturn(false);

    assertTrue(cartaService.estaNoCache(ID));
    assertFalse(cartaService.estaNoCache("outro"));
    assertFalse(cartaService.estaNoCache(null));
    assertFalse(cartaService.estaNoCache(""));
  }

  @Test
  @DisplayName("Deve retornar o TTL restante da carta no cache")
  void testTempoRestanteNoCache() {
    when(cartaRepository.tempoRestante(ID)).thenReturn(900L);
    when(cartaRepository.tempoRestante("expirada")).thenReturn(-2L);

    assertEquals(900L, cartaService.tempoRestanteNoCache(ID));
    assertEquals(-2L, cartaService.tempoRestanteNoCache("expirada"));
    assertEquals(-2L, cartaService.tempoRestanteNoCache(null));
  }

  @Test
  @DisplayName("Deve invalidar a carta do cache quando ela existir")
  void testInvalidarExistente() {
    when(cartaRepository.existe(ID)).thenReturn(true);

    assertTrue(cartaService.invalidar(ID));

    verify(cartaRepository, times(1)).evict(eq(ID));
  }

  @Test
  @DisplayName("Deve retornar false e não remover nada quando a carta não estiver no cache")
  void testInvalidarInexistente() {
    when(cartaRepository.existe("outra")).thenReturn(false);

    assertFalse(cartaService.invalidar("outra"));

    verify(cartaRepository, never()).evict(anyString());
  }
}
