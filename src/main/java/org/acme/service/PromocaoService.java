package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.acme.api.dto.PromocaoDTO;
import org.acme.domain.Promocao;
import org.acme.repository.PromocaoRepository;

/** Camada de serviço contendo as regras de negócio para gestão de Promoções. */
@ApplicationScoped
@AllArgsConstructor(onConstructor_ = @Inject)
public class PromocaoService {

  private final PromocaoRepository promocaoRepository;

  public void adicionar(PromocaoDTO promocaoDTO) {
    if (promocaoDTO == null) {
      throw new IllegalArgumentException("Os dados da promoção não podem ser nulos");
    }
    promocaoRepository.add(promocaoDTO.asPromocao());
  }

  public Optional<PromocaoDTO> consultar(String nomePromocao) {
    if (nomePromocao == null || nomePromocao.isBlank()) {
      return Optional.empty();
    }
    return promocaoRepository.get(nomePromocao).map(PromocaoDTO::from);
  }

  public Boolean verificaExistencia(String nomePromocao) {
    if (nomePromocao == null || nomePromocao.isBlank()) {
      return false;
    }
    return promocaoRepository.exists(nomePromocao);
  }

  public Long tempoRestanteDe(String nomePromocao) {
    if (nomePromocao == null || nomePromocao.isBlank()) {
      return -2L;
    }
    return promocaoRepository.tempoRestante(nomePromocao);
  }

  public List<PromocaoDTO> listar() {
    List<Promocao> promocoes = promocaoRepository.listar();
    if (promocoes == null || promocoes.isEmpty()) {
      return Collections.emptyList();
    }
    return promocoes.stream().map(PromocaoDTO::from).collect(Collectors.toList());
  }

  public Optional<PromocaoDTO> prorrogar(
      String nomePromocao, java.time.LocalDateTime novaDataExpiracao) {
    if (nomePromocao == null || nomePromocao.isBlank() || novaDataExpiracao == null) {
      return Optional.empty();
    }
    return promocaoRepository.prorrogar(nomePromocao, novaDataExpiracao).map(PromocaoDTO::from);
  }
}
