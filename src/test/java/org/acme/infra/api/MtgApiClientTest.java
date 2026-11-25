package org.acme.infra.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.acme.domain.Carta;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/**
 * Teste de integração do REST Client contra um servidor stub: valida o
 * mapeamento JSON (envelope {@code {"card": ...}}) e a semântica de erros do
 * cliente (WebApplicationException para HTTP >= 400).
 */
@QuarkusTest
@QuarkusTestResource(value = StubMtgApi.class, restrictToAnnotatedClass = true)
class MtgApiClientTest {

  @Inject
  @RestClient
  MtgApiClient mtgApiClient;

  @Test
  @DisplayName("Deve buscar a carta e mapear o envelope {\"card\": ...} para o domínio")
  void testBuscarPorId() {
    MtgApiClient.RespostaCarta resposta = mtgApiClient.buscarPorId("386616");

    assertNotNull(resposta);
    Carta carta = resposta.card();
    assertNotNull(carta);
    assertEquals("Narset, Enlightened Master", carta.getName());
    assertEquals("Mythic Rare", carta.getRarity());
    assertEquals(6.0d, carta.getCmc().doubleValue());
    assertEquals(List.of("White", "Blue", "Red"), carta.getColors());
    assertEquals(1, carta.getLegalidades().size());
    assertEquals("Commander", carta.getLegalidades().get(0).getFormat());
  }

  @Test
  @DisplayName("Deve lançar WebApplicationException com status 404 quando a API não encontrar a carta")
  void testBuscarPorIdNaoEncontrada() {
    WebApplicationException excecao = assertThrows(WebApplicationException.class,
        () -> mtgApiClient.buscarPorId("nao-existe"));

    assertEquals(404, excecao.getResponse().getStatus());
  }

  @Test
  @DisplayName("Deve lançar WebApplicationException com status 500 quando a API falhar")
  void testBuscarPorIdErroInterno() {
    WebApplicationException excecao = assertThrows(WebApplicationException.class,
        () -> mtgApiClient.buscarPorId("erro"));

    assertEquals(500, excecao.getResponse().getStatus());
  }
}
