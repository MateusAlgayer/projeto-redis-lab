package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.acme.api.dto.PromocaoDTO;
import org.acme.domain.Promocao;
import org.acme.domain.PromocaoInvalidaException;
import org.acme.domain.PromocaoJaExisteException;
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
    validarPercentual(promocaoDTO.getPercentualDesconto());
    validarExpiracao(promocaoDTO.getDataExpiracao());
    if (!promocaoRepository.add(promocaoDTO.asPromocao())) {
      throw new PromocaoJaExisteException(promocaoDTO.getNome());
    }
  }

  private void validarPercentual(Double percentual) {
    // A comparação negada também rejeita NaN.
    if (percentual == null || !(percentual >= 0 && percentual <= 100)) {
      throw new PromocaoInvalidaException("O percentual de desconto deve estar entre 0 e 100");
    }
  }

  private void validarExpiracao(LocalDateTime dataExpiracao) {
    if (dataExpiracao == null || !dataExpiracao.isAfter(LocalDateTime.now())) {
      throw new PromocaoInvalidaException("A data de expiração é obrigatória e deve estar no futuro");
    }
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
      String nomePromocao, LocalDateTime novaDataExpiracao) {
    if (nomePromocao == null || nomePromocao.isBlank() || novaDataExpiracao == null) {
      return Optional.empty();
    }
    validarExpiracao(novaDataExpiracao);
    return promocaoRepository.prorrogar(nomePromocao, novaDataExpiracao).map(PromocaoDTO::from);
  }
}
