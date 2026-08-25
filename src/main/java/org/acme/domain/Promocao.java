package org.acme.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade de domínio para representar uma Promoção.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promocao {
  private String nome;
  private Double percentualDesconto;
  private LocalDateTime dataExpiracao;

  public Double getDesconto() {
    return this.percentualDesconto;
  }
}



