package org.acme.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

  // Atalho de leitura: não faz parte do JSON gravado. A anotação também faz a
  // leitura ignorar o campo em dados gravados antes.
  @JsonIgnore
  public Double getDesconto() {
    return this.percentualDesconto;
  }
}



