package org.acme.api;

import java.util.List;

import org.acme.api.dto.PromocaoDTO;
import org.acme.service.PromocaoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.acme.api.dto.ProrrogarPromocaoDTO;

/**
 * Endpoints REST para gerenciamento de promoções com Redis.
 */
@Tag(name = "Promoções", description = "Operações para gerenciamento do ciclo de vida de promoções com cache Redis")
@RequestScoped
@Path("/promocao")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromocaoResource {

  private final PromocaoService promocaoService;

  public PromocaoResource(PromocaoService promocaoService) {
    this.promocaoService = promocaoService;
  }

  @POST
  @Operation(summary = "Adicionar uma nova promoção", description = "Persiste a promoção informada no Redis com TTL automático calculado até a data de expiração e retorna a URI do recurso criado no cabeçalho Location.")
  @APIResponses({
      @APIResponse(responseCode = "201", description = "Promoção criada com sucesso", headers = @Header(name = "Location", description = "URI do recurso criado", schema = @Schema(type = SchemaType.STRING))),
      @APIResponse(responseCode = "400", description = "Dados da promoção inválidos ou incompletos")
  })
  public RestResponse<Void> adicionar(
      @RequestBody(description = "Dados da promoção a ser criada", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PromocaoDTO.class))) PromocaoDTO promocao,
      @Context UriInfo uriInfo) {
    if (promocao == null || promocao.getNome() == null || promocao.getNome().isBlank()) {
      return RestResponse.status(RestResponse.Status.BAD_REQUEST);
    }
    promocaoService.adicionar(promocao);
    return RestResponse.created(uriInfo.getAbsolutePathBuilder().path(promocao.getNome()).build());
  }

  @GET
  @Path("/{nomePromocao}")
  @Operation(summary = "Consultar promoção por nome", description = "Recupera os detalhes completos de uma promoção existente com o nome informado.")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Promoção encontrada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PromocaoDTO.class))),
      @APIResponse(responseCode = "404", description = "Promoção não encontrada ou já expirada")
  })
  public RestResponse<PromocaoDTO> consultar(
      @Parameter(name = "nomePromocao", description = "Nome identificador da promoção", required = true, example = "blackfriday") @PathParam("nomePromocao") String nomePromocao) {
    return promocaoService.consultar(nomePromocao)
        .map(RestResponse::ok)
        .orElseGet(RestResponse::notFound);
  }

  @GET
  @Path("/existe/{nomePromocao}")
  @Operation(summary = "Verificar se promoção existe", description = "Verifica se uma promoção com o nome informado está cadastrada e ativa no Redis.")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Promoção existe e está ativa no cache"),
      @APIResponse(responseCode = "404", description = "Promoção não encontrada ou expirada")
  })
  public RestResponse<Void> consultarPromocaoExistente(
      @Parameter(name = "nomePromocao", description = "Nome identificador da promoção", required = true, example = "blackfriday") @PathParam("nomePromocao") String nomePromocao) {
    final Boolean existe = promocaoService.verificaExistencia(nomePromocao);
    if (Boolean.TRUE.equals(existe)) {
      return RestResponse.ok();
    } else {
      return RestResponse.notFound();
    }
  }

  @GET
  @Path("/tempoRestante/{nomePromocao}")
  @Operation(summary = "Obter tempo restante (TTL) de uma promoção", description = "Retorna o tempo restante de validade em segundos para a promoção informada.")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Tempo restante em segundos retornado com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.INTEGER))),
      @APIResponse(responseCode = "404", description = "Promoção não encontrada ou expirada")
  })
  public RestResponse<Long> tempoRestantePromocao(
      @Parameter(name = "nomePromocao", description = "Nome identificador da promoção", required = true, example = "blackfriday") @PathParam("nomePromocao") String nomePromocao) {
    final Long tempoRestante = promocaoService.tempoRestanteDe(nomePromocao);
    if (tempoRestante == null || tempoRestante == -2) {
      return RestResponse.notFound();
    } else {
      return RestResponse.ok(tempoRestante);
    }
  }

  @GET
  @Path("/listar")
  @Operation(summary = "Listar todas as promoções", description = "Retorna a listagem de todas as promoções ativas cadastradas no Redis.")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Lista de promoções ativas", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = PromocaoDTO.class)))
  })
  public RestResponse<List<PromocaoDTO>> listar() {
    return RestResponse.ok(promocaoService.listar());
  }

  @PUT
  @Path("/prorrogar/{nomePromocao}")
  @Operation(summary = "Prorrogar validade de uma promoção", description = "Atualiza a data/hora de expiração e redefine o TTL no cache Redis para a promoção identificada pelo nome informado.")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Promoção prorrogada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PromocaoDTO.class))),
      @APIResponse(responseCode = "400", description = "Payload inválido ou nova data de expiração não informada"),
      @APIResponse(responseCode = "404", description = "Promoção não encontrada ou já expirada")
  })
  public RestResponse<PromocaoDTO> prorrogar(
      @Parameter(name = "nomePromocao", description = "Nome identificador da promoção a ser prorrogada", required = true, example = "blackfriday") @PathParam("nomePromocao") String nomePromocao,
      @RequestBody(description = "Nova data e hora de expiração para a promoção", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProrrogarPromocaoDTO.class))) ProrrogarPromocaoDTO dto) {
    if (dto == null || dto.getNovaDataExpiracao() == null) {
      return RestResponse.status(RestResponse.Status.BAD_REQUEST);
    }
    return promocaoService.prorrogar(nomePromocao, dto.getNovaDataExpiracao())
        .map(RestResponse::ok)
        .orElseGet(RestResponse::notFound);
  }
}
