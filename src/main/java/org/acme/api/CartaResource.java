package org.acme.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.api.dto.CartaCacheDTO;
import org.acme.infra.api.MtgApiException;
import org.acme.service.CartaService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Endpoints REST do cache-aside de cartas de Magic: The Gathering. */
@Tag(
    name = "Cartas",
    description =
        "Consulta de cartas de Magic: The Gathering com cache Redis no padrão cache-aside")
@RequestScoped
@Path("/carta")
@Produces(MediaType.APPLICATION_JSON)
public class CartaResource {

  /** Cabeçalho que expõe se a resposta veio do cache. */
  public static final String HEADER_CACHE = "X-Cache";

  private final CartaService cartaService;

  public CartaResource(CartaService cartaService) {
    this.cartaService = cartaService;
  }

  @GET
  @Path("/{idCarta}")
  @Operation(
      summary = "Consultar carta (cache-aside)",
      description =
          "Busca a carta no Redis; em caso de miss consulta a API Magic: The Gathering, grava o"
              + " resultado no cache com TTL e o retorna. O cabeçalho X-Cache indica HIT (cache) ou"
              + " MISS (API).")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Carta retornada com sucesso (X-Cache: HIT ou MISS)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CartaCacheDTO.class))),
    @APIResponse(
        responseCode = "404",
        description = "Carta não encontrada na API Magic: The Gathering"),
    @APIResponse(
        responseCode = "503",
        description = "API Magic: The Gathering indisponível (timeout, rate limit ou erro)")
  })
  public RestResponse<CartaCacheDTO> consultar(
      @Parameter(
              name = "idCarta",
              description = "Identificador da carta (SHA1 ou multiverseid)",
              required = true,
              example = "386616")
          @PathParam("idCarta")
          String idCarta) {
    return cartaService
        .buscarComCache(idCarta)
        .map(
            dto ->
                RestResponse.ResponseBuilder.ok(dto)
                    .header(HEADER_CACHE, dto.isCacheHit() ? "HIT" : "MISS")
                    .build())
        .orElseGet(RestResponse::notFound);
  }

  @GET
  @Path("/{idCarta}/cache")
  @Operation(
      summary = "Verificar se a carta está no cache",
      description =
          "Retorna HTTP 200 se a carta já foi populada no Redis e ainda não expirou, caso contrário"
              + " HTTP 404.")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "Carta presente no cache"),
    @APIResponse(responseCode = "404", description = "Carta ausente do cache")
  })
  public RestResponse<Void> verificarCache(
      @Parameter(
              name = "idCarta",
              description = "Identificador da carta (SHA1 ou multiverseid)",
              required = true,
              example = "386616")
          @PathParam("idCarta")
          String idCarta) {
    return cartaService.estaNoCache(idCarta) ? RestResponse.ok() : RestResponse.notFound();
  }

  @GET
  @Path("/{idCarta}/cache/tempoRestante")
  @Operation(
      summary = "Obter TTL da carta no cache",
      description = "Retorna em segundos quanto tempo ainda dura a entrada da carta no Redis.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "TTL em segundos retornado com sucesso",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = SchemaType.INTEGER))),
    @APIResponse(responseCode = "404", description = "Carta ausente do cache")
  })
  public RestResponse<Long> tempoRestanteCache(
      @Parameter(
              name = "idCarta",
              description = "Identificador da carta (SHA1 ou multiverseid)",
              required = true,
              example = "386616")
          @PathParam("idCarta")
          String idCarta) {
    final long tempoRestante = cartaService.tempoRestanteNoCache(idCarta);
    if (tempoRestante < 0) {
      return RestResponse.notFound();
    }
    return RestResponse.ok(tempoRestante);
  }

  @DELETE
  @Path("/{idCarta}/cache")
  @Operation(
      summary = "Invalidar cache da carta",
      description =
          "Remove a entrada da carta do Redis (evict), forçando a próxima consulta a consultar a"
              + " API novamente.")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Cache invalidado com sucesso"),
    @APIResponse(responseCode = "404", description = "Carta ausente do cache")
  })
  public RestResponse<Void> invalidarCache(
      @Parameter(
              name = "idCarta",
              description = "Identificador da carta (SHA1 ou multiverseid)",
              required = true,
              example = "386616")
          @PathParam("idCarta")
          String idCarta) {
    return cartaService.invalidar(idCarta) ? RestResponse.noContent() : RestResponse.notFound();
  }

  @ServerExceptionMapper
  public RestResponse<Void> mapearErroApi(MtgApiException e) {
    return RestResponse.status(RestResponse.Status.SERVICE_UNAVAILABLE);
  }
}
