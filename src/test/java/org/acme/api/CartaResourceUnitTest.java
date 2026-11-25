package org.acme.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.acme.api.dto.CartaCacheDTO;
import org.acme.domain.Carta;
import org.acme.service.CartaService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartaResourceUnitTest {

  private CartaService cartaService;
  private CartaResource cartaResource;

  @BeforeEach
  void setUp() {
    cartaService = mock(CartaService.class);
    cartaResource = new CartaResource(cartaService);
  }

  private Carta cartaExemplo() {
    return Carta.builder().id("386616").name("Narset, Enlightened Master").build();
  }

  @Test
  @DisplayName("Deve retornar 200 com X-Cache HIT quando a carta vier do Redis")
  void testConsultarCacheHit() {
    when(cartaService.buscarComCache("386616"))
        .thenReturn(Optional.of(CartaCacheDTO.hit(cartaExemplo(), 1500L)));

    RestResponse<CartaCacheDTO> response = cartaResource.consultar("386616");

    assertEquals(200, response.getStatus());
    assertEquals("HIT", response.getHeaderString(CartaResource.HEADER_CACHE));
    assertEquals("REDIS", response.getEntity().getFonte());
    assertEquals(1500L, response.getEntity().getTempoRestanteCache());
  }

  @Test
  @DisplayName("Deve retornar 200 com X-Cache MISS quando a carta vier da API")
  void testConsultarCacheMiss() {
    when(cartaService.buscarComCache("386616"))
        .thenReturn(Optional.of(CartaCacheDTO.miss(cartaExemplo(), 3600L)));

    RestResponse<CartaCacheDTO> response = cartaResource.consultar("386616");

    assertEquals(200, response.getStatus());
    assertEquals("MISS", response.getHeaderString(CartaResource.HEADER_CACHE));
    assertEquals("API", response.getEntity().getFonte());
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carta não for encontrada")
  void testConsultarInexistente() {
    when(cartaService.buscarComCache("inexistente")).thenReturn(Optional.empty());

    RestResponse<CartaCacheDTO> response = cartaResource.consultar("inexistente");

    assertEquals(404, response.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 200 quando a carta estiver no cache")
  void testVerificarCachePresente() {
    when(cartaService.estaNoCache("386616")).thenReturn(true);

    assertEquals(200, cartaResource.verificarCache("386616").getStatus());
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carta não estiver no cache")
  void testVerificarCacheAusente() {
    when(cartaService.estaNoCache("inexistente")).thenReturn(false);

    assertEquals(404, cartaResource.verificarCache("inexistente").getStatus());
  }

  @Test
  @DisplayName("Deve retornar 200 com o TTL quando houver tempo restante no cache")
  void testTempoRestanteCache() {
    when(cartaService.tempoRestanteNoCache("386616")).thenReturn(900L);

    RestResponse<Long> response = cartaResource.tempoRestanteCache("386616");

    assertEquals(200, response.getStatus());
    assertEquals(900L, response.getEntity());
  }

  @Test
  @DisplayName("Deve retornar 404 quando não houver cache da carta")
  void testTempoRestanteCacheInexistente() {
    when(cartaService.tempoRestanteNoCache("inexistente")).thenReturn(-2L);

    assertEquals(404, cartaResource.tempoRestanteCache("inexistente").getStatus());
  }

  @Test
  @DisplayName("Deve retornar 204 ao invalidar o cache de uma carta existente")
  void testInvalidarCacheExistente() {
    when(cartaService.invalidar("386616")).thenReturn(true);

    assertEquals(204, cartaResource.invalidarCache("386616").getStatus());
  }

  @Test
  @DisplayName("Deve retornar 404 ao invalidar o cache de uma carta ausente")
  void testInvalidarCacheInexistente() {
    when(cartaService.invalidar("inexistente")).thenReturn(false);

    assertEquals(404, cartaResource.invalidarCache("inexistente").getStatus());
  }
}
