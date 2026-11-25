package org.acme.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade de domínio que representa uma carta de Magic: The Gathering
 * recuperada da API magicthegathering.io.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Carta {
  private String id;
  private String name;
  private List<String> names;
  private String manaCost;
  private Double cmc;
  private List<String> colors;
  private List<String> colorIdentity;
  private String type;
  private List<String> supertypes;
  private List<String> types;
  private List<String> subtypes;
  private String rarity;
  private String set;
  private String setName;
  private String text;
  private String flavor;
  private String artist;
  private String number;
  private String power;
  private String toughness;
  private String layout;
  private Integer multiverseid;
  private String imageUrl;
  private List<String> printings;
  @JsonProperty("legalities")
  private List<Legalidade> legalidades;

  /**
   * Legalidade da carta em um formato de jogo (Standard, Commander, etc).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Legalidade {
    private String format;
    private String legality;
  }
}
