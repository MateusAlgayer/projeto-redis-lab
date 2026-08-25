package org.acme.api.dto;

import java.time.LocalDateTime;

import org.acme.domain.Promocao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de Transferência de Dados (DTO) para Promoção.
 */
@Schema(name = "PromocaoDTO", description = "Representação dos dados de uma promoção")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromocaoDTO {

  @Schema(description = "Nome identificador exclusivo da promoção", required = true)
  private String nome;

  @Schema(description = "Percentual de desconto aplicado pela promoção (0 a 100)", required = true, minimum = "0.0", maximum = "100.0")
  private Double percentualDesconto;

  @Schema(description = "Data e hora de expiração da promoção (formato ISO-8601: YYYY-MM-DDTHH:mm:ss)", required = true, format = "date-time")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime dataExpiracao;

  public Promocao asPromocao() {
    return new Promocao(nome, percentualDesconto, dataExpiracao);
  }

  public static PromocaoDTO from(Promocao promocao) {
    if (promocao == null) {
      return null;
    }
    return new PromocaoDTO(promocao.getNome(), promocao.getDesconto(), promocao.getDataExpiracao());
  }

  public static PromocaoDTO vazio() {
    return new PromocaoDTO("Promoção não existente", 0.0, LocalDateTime.MIN);
  }
}
