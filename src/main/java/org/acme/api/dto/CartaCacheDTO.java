package org.acme.api.dto;

import org.acme.domain.Carta;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de Transferência de Dados (DTO) para a resposta do cache-aside de
 * cartas, incluindo metadados sobre a origem dos dados.
 */
@Schema(name = "CartaCacheDTO", description = "Carta de Magic: The Gathering com informações de origem do cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartaCacheDTO {

  public static final String FONTE_REDIS = "REDIS";
  public static final String FONTE_API = "API";

  @Schema(description = "Dados completos da carta")
  private Carta carta;

  @Schema(description = "Indica se a carta veio do cache (true) ou da API externa (false)")
  private boolean cacheHit;

  @Schema(description = "Origem dos dados: REDIS (cache) ou API (fonte de dados)")
  private String fonte;

  @Schema(description = "Segundos restantes de validade da entrada no cache; nulo quando não há cache")
  private Long tempoRestanteCache;

  public static CartaCacheDTO hit(Carta carta, Long tempoRestanteCache) {
    return CartaCacheDTO.builder()
        .carta(carta)
        .cacheHit(true)
        .fonte(FONTE_REDIS)
        .tempoRestanteCache(tempoRestanteCache)
        .build();
  }

  public static CartaCacheDTO miss(Carta carta, long ttlSegundos) {
    return CartaCacheDTO.builder()
        .carta(carta)
        .cacheHit(false)
        .fonte(FONTE_API)
        .tempoRestanteCache(ttlSegundos)
        .build();
  }
}
