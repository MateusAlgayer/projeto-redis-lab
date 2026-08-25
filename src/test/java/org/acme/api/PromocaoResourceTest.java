package org.acme.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.greaterThan;

import java.time.LocalDateTime;

import org.acme.api.dto.PromocaoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PromocaoResourceTest {

  @Test
  @Order(1)
  @DisplayName("Deve cadastrar uma nova promoção com data contendo segundos com sucesso (HTTP 201)")
  void testAdicionarPromocao() {
    LocalDateTime dataExpiracao = LocalDateTime.now().plusHours(2).plusMinutes(30).plusSeconds(45).withNano(0);
    PromocaoDTO dto = new PromocaoDTO("blackfriday_2026", 40.0, dataExpiracao);

    given()
        .contentType(ContentType.JSON)
        .body(dto)
        .when()
        .post("/promocao")
        .then()
        .statusCode(201)
        .header("Location", org.hamcrest.Matchers.containsString("/promocao/blackfriday_2026"));
  }

  @Test
  @Order(2)
  @DisplayName("Deve retornar HTTP 400 ao tentar cadastrar promoção inválida")
  void testAdicionarPromocaoInvalida() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"nome\": \"\", \"percentualDesconto\": 10.0, \"dataExpiracao\": \"2026-12-31T23:59:59\"}")
        .when()
        .post("/promocao")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(3)
  @DisplayName("Deve consultar a promoção cadastrada por nome (HTTP 200)")
  void testConsultarPromocao() {
    given()
        .when()
        .get("/promocao/blackfriday_2026")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("nome", equalTo("blackfriday_2026"))
        .body("percentualDesconto", equalTo(40.0f));
  }

  @Test
  @Order(4)
  @DisplayName("Deve retornar HTTP 404 ao consultar promoção inexistente")
  void testConsultarPromocaoInexistente() {
    given()
        .when()
        .get("/promocao/promocao_inexistente_xyz")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(5)
  @DisplayName("Deve verificar se promoção existe (HTTP 200)")
  void testVerificarExistencia() {
    given()
        .when()
        .get("/promocao/existe/blackfriday_2026")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(6)
  @DisplayName("Deve retornar HTTP 404 ao verificar existência de promoção não cadastrada")
  void testVerificarExistenciaInexistente() {
    given()
        .when()
        .get("/promocao/existe/promocao_inexistente_xyz")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(7)
  @DisplayName("Deve obter tempo restante da promoção em segundos (HTTP 200)")
  void testTempoRestante() {
    given()
        .when()
        .get("/promocao/tempoRestante/blackfriday_2026")
        .then()
        .statusCode(200)
        .body(greaterThan("0"));
  }

  @Test
  @Order(8)
  @DisplayName("Deve retornar HTTP 404 para tempo restante de promoção inexistente")
  void testTempoRestanteInexistente() {
    given()
        .when()
        .get("/promocao/tempoRestante/promocao_inexistente_xyz")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(9)
  @DisplayName("Deve listar promoções cadastradas contendo a promoção recém-adicionada (HTTP 200)")
  void testListarPromocoes() {
    given()
        .when()
        .get("/promocao/listar")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("nome", hasItem("blackfriday_2026"));
  }

  @Test
  @Order(10)
  @DisplayName("Deve prorrogar uma promoção existente com sucesso usando PUT (HTTP 200)")
  void testProrrogarPromocao() {
    LocalDateTime novaData = LocalDateTime.now().plusDays(60).withNano(0);
    org.acme.api.dto.ProrrogarPromocaoDTO dto = new org.acme.api.dto.ProrrogarPromocaoDTO(novaData);

    given()
        .contentType(ContentType.JSON)
        .body(dto)
        .when()
        .put("/promocao/prorrogar/blackfriday_2026")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("nome", equalTo("blackfriday_2026"))
        .body("percentualDesconto", equalTo(40.0f));
  }

  @Test
  @Order(11)
  @DisplayName("Deve retornar HTTP 400 ao tentar prorrogar com dados inválidos")
  void testProrrogarPromocaoInvalida() {
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .put("/promocao/prorrogar/blackfriday_2026")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(12)
  @DisplayName("Deve retornar HTTP 404 ao tentar prorrogar promoção inexistente")
  void testProrrogarPromocaoInexistente() {
    LocalDateTime novaData = LocalDateTime.now().plusDays(60).withNano(0);
    org.acme.api.dto.ProrrogarPromocaoDTO dto = new org.acme.api.dto.ProrrogarPromocaoDTO(novaData);

    given()
        .contentType(ContentType.JSON)
        .body(dto)
        .when()
        .put("/promocao/prorrogar/promocao_inexistente_xyz")
        .then()
        .statusCode(404);
  }
}


