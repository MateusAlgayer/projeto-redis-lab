package org.acme.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromocaoTest {

  @Test
  @DisplayName("Deve criar Promocao via construtor padrão e setters")
  void testNoArgsConstructorAndSetters() {
    Promocao promocao = new Promocao();
    LocalDateTime data = LocalDateTime.of(2026, 8, 30, 14, 0, 0);
    promocao.setNome("queima_estoque");
    promocao.setPercentualDesconto(50.0);
    promocao.setDataExpiracao(data);

    assertEquals("queima_estoque", promocao.getNome());
    assertEquals(50.0, promocao.getPercentualDesconto());
    assertEquals(50.0, promocao.getDesconto());
    assertEquals(data, promocao.getDataExpiracao());
  }

  @Test
  @DisplayName("Deve criar Promocao via Builder")
  void testBuilder() {
    LocalDateTime data = LocalDateTime.of(2026, 9, 15, 20, 45, 30);
    Promocao promocao = Promocao.builder()
        .nome("dia_dos_pais")
        .percentualDesconto(20.0)
        .dataExpiracao(data)
        .build();

    assertNotNull(promocao);
    assertEquals("dia_dos_pais", promocao.getNome());
    assertEquals(20.0, promocao.getPercentualDesconto());
    assertEquals(data, promocao.getDataExpiracao());
  }
}

