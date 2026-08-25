package org.acme.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.acme.api.dto.PromocaoDTO;
import org.acme.service.PromocaoService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

class PromocaoResourceUnitTest {

  private PromocaoService promocaoService;
  private PromocaoResource promocaoResource;
  private UriInfo uriInfo;

  @BeforeEach
  void setUp() {
    promocaoService = mock(PromocaoService.class);
    promocaoResource = new PromocaoResource(promocaoService);
    uriInfo = mock(UriInfo.class);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromUri("http://localhost:8080/promocao"));
  }

  @Test
  @DisplayName("Deve retornar 201 Created ao adicionar promoção válida")
  void testAdicionarSucesso() {
    PromocaoDTO dto = new PromocaoDTO("pascoa", 15.0, LocalDateTime.of(2026, 4, 5, 10, 0, 0));

    RestResponse<Void> response = promocaoResource.adicionar(dto, uriInfo);

    assertEquals(201, response.getStatus());
    assertNotNull(response.getLocation());
    assertEquals(URI.create("http://localhost:8080/promocao/pascoa"), response.getLocation());
    verify(promocaoService, times(1)).adicionar(dto);
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request ao adicionar promoção com dados inválidos")
  void testAdicionarInvalido() {
    RestResponse<Void> responseNull = promocaoResource.adicionar(null, uriInfo);
    assertEquals(400, responseNull.getStatus());

    PromocaoDTO dtoNomeVazio = new PromocaoDTO("", 15.0, LocalDateTime.of(2026, 4, 5, 10, 0, 0));
    RestResponse<Void> responseVazio = promocaoResource.adicionar(dtoNomeVazio, uriInfo);
    assertEquals(400, responseVazio.getStatus());

    verify(promocaoService, never()).adicionar(any());
  }

  @Test
  @DisplayName("Deve retornar 200 OK ao consultar promoção existente")
  void testConsultarExistente() {
    PromocaoDTO dto = new PromocaoDTO("pascoa", 15.0, LocalDateTime.of(2026, 4, 5, 10, 0, 0));
    when(promocaoService.consultar("pascoa")).thenReturn(Optional.of(dto));

    RestResponse<PromocaoDTO> response = promocaoResource.consultar("pascoa");

    assertEquals(200, response.getStatus());
    assertEquals(dto, response.getEntity());
  }

  @Test
  @DisplayName("Deve retornar 404 Not Found ao consultar promoção inexistente")
  void testConsultarInexistente() {
    when(promocaoService.consultar("inexistente")).thenReturn(Optional.empty());

    RestResponse<PromocaoDTO> response = promocaoResource.consultar("inexistente");

    assertEquals(404, response.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 200 OK quando a promoção existir no cache")
  void testConsultarPromocaoExistenteTrue() {
    when(promocaoService.verificaExistencia("pascoa")).thenReturn(true);

    RestResponse<Void> response = promocaoResource.consultarPromocaoExistente("pascoa");

    assertEquals(200, response.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 404 Not Found quando a promoção não existir no cache")
  void testConsultarPromocaoExistenteFalse() {
    when(promocaoService.verificaExistencia("inexistente")).thenReturn(false);

    RestResponse<Void> response = promocaoResource.consultarPromocaoExistente("inexistente");

    assertEquals(404, response.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 200 OK com TTL quando houver tempo restante")
  void testTempoRestantePromocaoSucesso() {
    when(promocaoService.tempoRestanteDe("pascoa")).thenReturn(5000L);

    RestResponse<Long> response = promocaoResource.tempoRestantePromocao("pascoa");

    assertEquals(200, response.getStatus());
    assertEquals(5000L, response.getEntity());
  }

  @Test
  @DisplayName("Deve retornar 404 Not Found quando a promoção não tiver TTL ou for inexistente")
  void testTempoRestantePromocaoInexistente() {
    when(promocaoService.tempoRestanteDe("inexistente")).thenReturn(-2L);

    RestResponse<Long> response = promocaoResource.tempoRestantePromocao("inexistente");

    assertEquals(404, response.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 200 OK com lista de promoções")
  void testListar() {
    PromocaoDTO p1 = new PromocaoDTO("p1", 10.0, LocalDateTime.of(2026, 12, 1, 12, 0, 0));
    when(promocaoService.listar()).thenReturn(List.of(p1));

    RestResponse<List<PromocaoDTO>> response = promocaoResource.listar();

    assertEquals(200, response.getStatus());
    assertEquals(1, response.getEntity().size());
  }

  @Test
  @DisplayName("Deve retornar 200 OK com promoção atualizada ao prorrogar com sucesso")
  void testProrrogarSucesso() {
    LocalDateTime novaData = LocalDateTime.of(2027, 1, 1, 0, 0, 0);
    org.acme.api.dto.ProrrogarPromocaoDTO dto = new org.acme.api.dto.ProrrogarPromocaoDTO(novaData);
    PromocaoDTO atualizada = new PromocaoDTO("pascoa", 15.0, novaData);
    when(promocaoService.prorrogar("pascoa", novaData)).thenReturn(Optional.of(atualizada));

    RestResponse<PromocaoDTO> response = promocaoResource.prorrogar("pascoa", dto);

    assertEquals(200, response.getStatus());
    assertEquals(atualizada, response.getEntity());
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request ao prorrogar com payload nulo ou data nula")
  void testProrrogarInvalido() {
    RestResponse<PromocaoDTO> res1 = promocaoResource.prorrogar("pascoa", null);
    assertEquals(400, res1.getStatus());

    RestResponse<PromocaoDTO> res2 = promocaoResource.prorrogar("pascoa", new org.acme.api.dto.ProrrogarPromocaoDTO(null));
    assertEquals(400, res2.getStatus());
  }

  @Test
  @DisplayName("Deve retornar 404 Not Found ao tentar prorrogar promoção inexistente")
  void testProrrogarInexistente() {
    LocalDateTime novaData = LocalDateTime.of(2027, 1, 1, 0, 0, 0);
    org.acme.api.dto.ProrrogarPromocaoDTO dto = new org.acme.api.dto.ProrrogarPromocaoDTO(novaData);
    when(promocaoService.prorrogar("inexistente", novaData)).thenReturn(Optional.empty());

    RestResponse<PromocaoDTO> response = promocaoResource.prorrogar("inexistente", dto);

    assertEquals(404, response.getStatus());
  }
}


