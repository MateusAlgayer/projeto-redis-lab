package org.acme.infra.api;

import org.acme.domain.Carta;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST Client (MicroProfile REST Client) da API Magic: The Gathering.
 *
 * A implementação HTTP é gerada pelo Quarkus a partir desta interface: basta
 * anotar com {@link RegisterRestClient} e injetar com {@code @RestClient}.
 * A base URL e os timeouts são resolvidos pelas propriedades
 * {@code quarkus.rest-client.mtgApiUrl.*}.
 *
 * Em caso de resposta HTTP >= 400 o cliente lança
 * {@link jakarta.ws.rs.WebApplicationException}; falhas de rede/timeout lançam
 * {@link jakarta.ws.rs.ProcessingException}. A tradução para o domínio é feita
 * em {@code CartaService}.
 */
@Path("/cards")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "mtgApiUrl")
public interface MtgApiClient {

  /**
   * Busca uma carta pelo identificador aceito pela API (SHA1 do card ou
   * multiverseid).
   *
   * @param id identificador da carta
   * @return o envelope de resposta contendo a carta
   * @throws jakarta.ws.rs.WebApplicationException quando a API responde HTTP >= 400
   */
  @GET
  @Path("/{id}")
  RespostaCarta buscarPorId(@PathParam("id") String id);

  /**
   * Envelope de resposta da API: {@code {"card": {...}}}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record RespostaCarta(Carta card) {
  }
}
