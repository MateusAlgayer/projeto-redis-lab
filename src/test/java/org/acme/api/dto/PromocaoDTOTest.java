package org.acme.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.acme.domain.Promocao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromocaoDTOTest {

  @Test
  @DisplayName("Deve converter PromocaoDTO para Promocao")
  void testAsPromocao() {
    LocalDateTime dataExpiracao = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
    PromocaoDTO dto = new PromocaoDTO("blackfriday", 25.0, dataExpiracao);

    Promocao domain = dto.asPromocao();

    assertNotNull(domain);
    assertEquals("blackfriday", domain.getNome());
    assertEquals(25.0, domain.getPercentualDesconto());
    assertEquals(25.0, domain.getDesconto());
    assertEquals(dataExpiracao, domain.getDataExpiracao());
  }

  @Test
  @DisplayName("Deve converter Promocao para PromocaoDTO")
  void testFromPromocao() {
    LocalDateTime dataExpiracao = LocalDateTime.of(2026, 11, 20, 18, 30, 0);
    Promocao domain = new Promocao("natal", 15.0, dataExpiracao);

    PromocaoDTO dto = PromocaoDTO.from(domain);

    assertNotNull(dto);
    assertEquals("natal", dto.getNome());
    assertEquals(15.0, dto.getPercentualDesconto());
    assertEquals(dataExpiracao, dto.getDataExpiracao());
  }

  @Test
  @DisplayName("Deve retornar null ao converter Promocao nula")
  void testFromNullPromocao() {
    assertNull(PromocaoDTO.from(null));
  }

  @Test
  @DisplayName("Deve gerar PromocaoDTO vazio corretamente")
  void testVazio() {
    PromocaoDTO vazio = PromocaoDTO.vazio();
    assertNotNull(vazio);
    assertEquals("Promoção não existente", vazio.getNome());
    assertEquals(0.0, vazio.getPercentualDesconto());
    assertEquals(LocalDateTime.MIN, vazio.getDataExpiracao());
  }

  @Test
  @DisplayName("Deve validar builder e setters do PromocaoDTO")
  void testBuilderAndSetters() {
    LocalDateTime data = LocalDateTime.of(2026, 10, 10, 12, 0, 0);
    PromocaoDTO dto = PromocaoDTO.builder()
        .nome("pascoa")
        .percentualDesconto(10.0)
        .dataExpiracao(data)
        .build();

    assertEquals("pascoa", dto.getNome());
    assertEquals(10.0, dto.getPercentualDesconto());
    assertEquals(data, dto.getDataExpiracao());

    dto.setNome("pascoa_editada");
    dto.setPercentualDesconto(12.0);
    assertEquals("pascoa_editada", dto.getNome());
    assertEquals(12.0, dto.getPercentualDesconto());
  }
}

