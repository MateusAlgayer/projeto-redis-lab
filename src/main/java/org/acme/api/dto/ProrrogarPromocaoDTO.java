package org.acme.api.dto;

import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com os parâmetros necessários para prorrogação de uma promoção existente.
 */
@Schema(name = "ProrrogarPromocaoDTO", description = "Parâmetros para prorrogação da validade de uma promoção")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrrogarPromocaoDTO {

  @Schema(
      description = "Nova data e hora de expiração da promoção (formato ISO-8601: YYYY-MM-DDTHH:mm:ss)",
      required = true,
      format = "date-time"
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime novaDataExpiracao;
}
