package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.acme.api.dto.PromocaoDTO;
import org.acme.domain.Promocao;
import org.acme.repository.PromocaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PromocaoServiceTest {

  private PromocaoRepository promocaoRepository;
  private PromocaoService promocaoService;

  @BeforeEach
  void setUp() {
    promocaoRepository = Mockito.mock(PromocaoRepository.class);
    promocaoService = new PromocaoService(promocaoRepository);
  }

  @Test
  @DisplayName("Deve adicionar promoção com sucesso")
  void testAdicionarSucesso() {
    PromocaoDTO dto = new PromocaoDTO("cybermonday", 30.0, LocalDateTime.of(2026, 11, 30, 23, 59, 59));

    promocaoService.adicionar(dto);

    verify(promocaoRepository, times(1)).add(any(Promocao.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar adicionar promoção nula")
  void testAdicionarNulo() {
    assertThrows(IllegalArgumentException.class, () -> promocaoService.adicionar(null));
    verify(promocaoRepository, never()).add(any());
  }

  @Test
  @DisplayName("Deve consultar promoção existente por nome")
  void testConsultarExistente() {
    Promocao promo = new Promocao("cybermonday", 30.0, LocalDateTime.of(2026, 11, 30, 23, 59, 59));
    when(promocaoRepository.get("cybermonday")).thenReturn(Optional.of(promo));

    Optional<PromocaoDTO> resultado = promocaoService.consultar("cybermonday");

    assertTrue(resultado.isPresent());
    assertEquals("cybermonday", resultado.get().getNome());
    assertEquals(30.0, resultado.get().getPercentualDesconto());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao consultar promoção inexistente")
  void testConsultarInexistente() {
    when(promocaoRepository.get("inexistente")).thenReturn(Optional.empty());

    Optional<PromocaoDTO> resultado = promocaoService.consultar("inexistente");

    assertTrue(resultado.isEmpty());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao consultar com nome nulo ou vazio")
  void testConsultarNomeInvalido() {
    assertTrue(promocaoService.consultar(null).isEmpty());
    assertTrue(promocaoService.consultar("   ").isEmpty());
    verify(promocaoRepository, never()).get(any());
  }

  @Test
  @DisplayName("Deve verificar existência de promoção com sucesso")
  void testVerificaExistencia() {
    when(promocaoRepository.exists("cybermonday")).thenReturn(true);
    when(promocaoRepository.exists("inexistente")).thenReturn(false);

    assertTrue(promocaoService.verificaExistencia("cybermonday"));
    assertFalse(promocaoService.verificaExistencia("inexistente"));
    assertFalse(promocaoService.verificaExistencia(null));
    assertFalse(promocaoService.verificaExistencia(""));
  }

  @Test
  @DisplayName("Deve obter tempo restante de uma promoção")
  void testTempoRestanteDe() {
    when(promocaoRepository.tempoRestante("cybermonday")).thenReturn(3600L);
    when(promocaoRepository.tempoRestante("inexistente")).thenReturn(-2L);

    assertEquals(3600L, promocaoService.tempoRestanteDe("cybermonday"));
    assertEquals(-2L, promocaoService.tempoRestanteDe("inexistente"));
    assertEquals(-2L, promocaoService.tempoRestanteDe(null));
    assertEquals(-2L, promocaoService.tempoRestanteDe("   "));
  }

  @Test
  @DisplayName("Deve listar todas as promoções cadastradas")
  void testListar() {
    Promocao p1 = new Promocao("promo1", 10.0, LocalDateTime.of(2026, 12, 1, 0, 0, 0));
    Promocao p2 = new Promocao("promo2", 20.0, LocalDateTime.of(2026, 12, 2, 0, 0, 0));
    when(promocaoRepository.listar()).thenReturn(List.of(p1, p2));

    List<PromocaoDTO> resultado = promocaoService.listar();

    assertNotNull(resultado);
    assertEquals(2, resultado.size());
    assertEquals("promo1", resultado.get(0).getNome());
    assertEquals("promo2", resultado.get(1).getNome());
  }

  @Test
  @DisplayName("Deve retornar lista vazia quando o repositório retornar vazio ou nulo")
  void testListarVazio() {
    when(promocaoRepository.listar()).thenReturn(Collections.emptyList());
    assertTrue(promocaoService.listar().isEmpty());

    when(promocaoRepository.listar()).thenReturn(null);
    assertTrue(promocaoService.listar().isEmpty());
  }

  @Test
  @DisplayName("Deve prorrogar promoção com sucesso")
  void testProrrogarSucesso() {
    LocalDateTime novaData = LocalDateTime.of(2027, 1, 15, 23, 59, 59);
    Promocao promoAtualizada = new Promocao("cybermonday", 30.0, novaData);
    when(promocaoRepository.prorrogar("cybermonday", novaData)).thenReturn(Optional.of(promoAtualizada));

    Optional<PromocaoDTO> resultado = promocaoService.prorrogar("cybermonday", novaData);

    assertTrue(resultado.isPresent());
    assertEquals(novaData, resultado.get().getDataExpiracao());
  }

  @Test
  @DisplayName("Deve retornar Optional vazio ao prorrogar promoção inexistente ou com parâmetros inválidos")
  void testProrrogarInvalido() {
    LocalDateTime novaData = LocalDateTime.of(2027, 1, 15, 23, 59, 59);
    when(promocaoRepository.prorrogar("inexistente", novaData)).thenReturn(Optional.empty());

    assertTrue(promocaoService.prorrogar("inexistente", novaData).isEmpty());
    assertTrue(promocaoService.prorrogar(null, novaData).isEmpty());
    assertTrue(promocaoService.prorrogar("   ", novaData).isEmpty());
    assertTrue(promocaoService.prorrogar("cybermonday", null).isEmpty());
  }
}


