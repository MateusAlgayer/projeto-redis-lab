package org.acme.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.acme.domain.Carta;
import org.acme.infra.api.MtgApiClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Teste de integração do cache-aside: a API externa é mockada e o Redis é real
 * (sobido pelo DevServices), permitindo validar o ciclo miss → gravação no
 * cache → hit → invalidação.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartaResourceTest {

  private static final String ID_CARTA = "cacheaside001";

  @InjectMock
  @RestClient
  MtgApiClient mtgApiClient;

  @BeforeEach
  void prepararRespostaDaApi() {
    Carta carta = Carta.builder()
        .id(ID_CARTA)
        .name("Narset, Enlightened Master")
        .manaCost("{3}{U}{R}{W}")
        .cmc(6.0)
        .rarity("Mythic Rare")
        .set("KTK")
        .build();
    when(mtgApiClient.buscarPorId(ID_CARTA)).thenReturn(new MtgApiClient.RespostaCarta(carta));
    when(mtgApiClient.buscarPorId("nao_existe_xyz"))
        .thenThrow(new WebApplicationException(Response.status(404).build()));
  }

  @Test
  @Order(1)
  @DisplayName("Deve consultar a API e popular o cache na primeira chamada (HTTP 200, X-Cache: MISS)")
  void testPrimeiraConsultaCacheMiss() {
    given()
        .when()
        .get("/carta/{id}", ID_CARTA)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("X-Cache", equalTo("MISS"))
        .body("fonte", equalTo("API"))
        .body("cacheHit", equalTo(false))
        .body("carta.name", equalTo("Narset, Enlightened Master"));
  }

  @Test
  @Order(2)
  @DisplayName("Deve servir a carta do Redis na segunda chamada sem consultar a API (HTTP 200, X-Cache: HIT)")
  void testSegundaConsultaCacheHit() {
    given()
        .when()
        .get("/carta/{id}", ID_CARTA)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("X-Cache", equalTo("HIT"))
        .body("fonte", equalTo("REDIS"))
        .body("cacheHit", equalTo(true))
        .body("tempoRestanteCache", greaterThanOrEqualTo(0))
        .body("carta.name", equalTo("Narset, Enlightened Master"));

    // prova de que os dados vieram do cache: a API externa não foi acionada
    verify(mtgApiClient, never()).buscarPorId(any());
  }

  @Test
  @Order(3)
  @DisplayName("Deve confirmar que a carta está no cache e expor o TTL (HTTP 200)")
  void testVerificarCacheETempoRestante() {
    given()
        .when()
        .get("/carta/{id}/cache", ID_CARTA)
        .then()
        .statusCode(200);

    int ttl = given()
        .when()
        .get("/carta/{id}/cache/tempoRestante", ID_CARTA)
        .then()
        .statusCode(200)
        .extract().as(Integer.class);

    assertTrue(ttl >= 0, "O TTL do cache deveria ser positivo, mas veio " + ttl);
  }

  @Test
  @Order(4)
  @DisplayName("Deve invalidar o cache da carta (HTTP 204) e reportar ausência em seguida (HTTP 404)")
  void testInvalidarCache() {
    given()
        .when()
        .delete("/carta/{id}/cache", ID_CARTA)
        .then()
        .statusCode(204);

    given()
        .when()
        .get("/carta/{id}/cache", ID_CARTA)
        .then()
        .statusCode(404);
  }

  @Test
  @Order(5)
  @DisplayName("Deve voltar a consultar a API após a invalidação (HTTP 200, X-Cache: MISS)")
  void testConsultaAposInvalidacao() {
    given()
        .when()
        .get("/carta/{id}", ID_CARTA)
        .then()
        .statusCode(200)
        .header("X-Cache", equalTo("MISS"))
        .body("fonte", equalTo("API"));
  }

  @Test
  @Order(6)
  @DisplayName("Deve retornar HTTP 404 quando a API não encontrar a carta")
  void testCartaInexistente() {
    given()
        .when()
        .get("/carta/{id}", "nao_existe_xyz")
        .then()
        .statusCode(404);

    given()
        .when()
        .get("/carta/{id}/cache", "nao_existe_xyz")
        .then()
        .statusCode(404);

    given()
        .when()
        .delete("/carta/{id}/cache", "nao_existe_xyz")
        .then()
        .statusCode(404);
  }
}
