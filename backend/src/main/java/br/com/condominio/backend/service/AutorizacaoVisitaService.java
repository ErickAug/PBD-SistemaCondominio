package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AutorizacaoVisita;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.enums.StatusAutorizacaoVisita;
import br.com.condominio.backend.repository.AutorizacaoVisitaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutorizacaoVisitaService {

    private final AutorizacaoVisitaRepository autorizacaoVisitaRepository;

    public AutorizacaoVisitaService(AutorizacaoVisitaRepository autorizacaoVisitaRepository) {
        this.autorizacaoVisitaRepository = autorizacaoVisitaRepository;
    }

    public AutorizacaoVisita cadastrar(Unidade unidade, String nome, String documento, String tipo,
                                        LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = dataFim != null ? dataFim : dataInicio;

        if (fim.isBefore(dataInicio)) {
            throw new RegraDeNegocioException("A data de fim não pode ser anterior à data de início.");
        }

        AutorizacaoVisita autorizacao = new AutorizacaoVisita();
        autorizacao.setUnidade(unidade);
        autorizacao.setNome(nome);
        autorizacao.setDocumento(documento);
        autorizacao.setTipo(tipo);
        autorizacao.setDataInicio(dataInicio);
        autorizacao.setDataFim(fim);
        autorizacao.setStatus(StatusAutorizacaoVisita.PENDENTE);

        return autorizacaoVisitaRepository.save(autorizacao);
    }

    public List<AutorizacaoVisita> listarPorUnidade(Long unidadeId) {
        return autorizacaoVisitaRepository.findByUnidadeId(unidadeId);
    }

    public AutorizacaoVisita cancelar(Long id, Long unidadeId) {
        AutorizacaoVisita autorizacao = autorizacaoVisitaRepository.findByIdAndUnidadeId(id, unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Autorização não encontrada."));

        if (autorizacao.getStatus() != StatusAutorizacaoVisita.PENDENTE) {
            throw new RegraDeNegocioException("Só é possível cancelar uma autorização que ainda não foi usada.");
        }

        autorizacao.setStatus(StatusAutorizacaoVisita.CANCELADA);
        return autorizacaoVisitaRepository.save(autorizacao);
    }

    public AutorizacaoVisita confirmarUso(Long condominioId, Long id) {
        AutorizacaoVisita autorizacao = autorizacaoVisitaRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Autorização não encontrada."));

        if (!autorizacao.getUnidade().getBloco().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Autorização não encontrada.");
        }

        if (autorizacao.getStatus() != StatusAutorizacaoVisita.PENDENTE) {
            throw new RegraDeNegocioException("Esta autorização não está disponível para uso.");
        }

        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(autorizacao.getDataInicio()) || hoje.isAfter(autorizacao.getDataFim())) {
            throw new RegraDeNegocioException("Esta autorização não é válida hoje.");
        }

        autorizacao.setStatus(StatusAutorizacaoVisita.USADA);
        autorizacao.setDataUso(LocalDateTime.now());
        return autorizacaoVisitaRepository.save(autorizacao);
    }

    public List<AutorizacaoVisita> buscarPorNomeOuDocumento(Long condominioId, String termo) {
        return autorizacaoVisitaRepository.buscarPorNomeOuDocumento(condominioId, termo);
    }

    public List<AutorizacaoVisita> listarEsperadasHoje(Long condominioId) {
        return autorizacaoVisitaRepository.listarEsperadasHoje(condominioId, LocalDate.now());
    }

    public String calcularSituacao(AutorizacaoVisita autorizacao) {
        LocalDate hoje = LocalDate.now();

        return switch (autorizacao.getStatus()) {
            case CANCELADA -> "CANCELADA";
            case USADA -> "USADA";
            case PENDENTE -> {
                if (hoje.isAfter(autorizacao.getDataFim())) {
                    yield "VENCIDA";
                } else if (hoje.isBefore(autorizacao.getDataInicio())) {
                    yield "AGUARDANDO";
                } else {
                    yield "LIBERADA";
                }
            }
        };
    }
}
