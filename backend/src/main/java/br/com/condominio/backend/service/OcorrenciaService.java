package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.OcorrenciaRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final CondominioRepository condominioRepository;
    private final UnidadeRepository unidadeRepository;

    public OcorrenciaService(OcorrenciaRepository ocorrenciaRepository,
                              CondominioRepository condominioRepository,
                              UnidadeRepository unidadeRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.condominioRepository = condominioRepository;
        this.unidadeRepository = unidadeRepository;
    }

    public Ocorrencia registrar(Long condominioId, String texto, String tipo, Long unidadeId, Usuario autor) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("Informe o texto da ocorrência.");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new RegraDeNegocioException("Informe o tipo da ocorrência.");
        }

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        Unidade unidade = buscarUnidadeDoCondominioOuNulo(condominioId, unidadeId);

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setCondominio(condominio);
        ocorrencia.setUnidade(unidade);
        ocorrencia.setTexto(texto);
        ocorrencia.setTipo(tipo);
        ocorrencia.setAutor(autor);
        ocorrencia.setDataHora(LocalDateTime.now());

        return ocorrenciaRepository.save(ocorrencia);
    }

    public Ocorrencia corrigir(Long condominioId, Long ocorrenciaId, String novoTexto, Usuario autor) {
        if (novoTexto == null || novoTexto.isBlank()) {
            throw new RegraDeNegocioException("Informe o texto da correção.");
        }

        Ocorrencia original = buscarPorIdNoCondominio(condominioId, ocorrenciaId);

        Ocorrencia correcao = new Ocorrencia();
        correcao.setCondominio(original.getCondominio());
        correcao.setUnidade(original.getUnidade());
        correcao.setTexto(novoTexto);
        correcao.setTipo(original.getTipo());
        correcao.setAutor(autor);
        correcao.setDataHora(LocalDateTime.now());
        correcao.setCorrecaoDe(original);

        return ocorrenciaRepository.save(correcao);
    }

    public Ocorrencia marcarResolvida(Long condominioId, Long ocorrenciaId, Usuario responsavel) {
        Ocorrencia ocorrencia = buscarPorIdNoCondominio(condominioId, ocorrenciaId);

        if (ocorrencia.isResolvida()) {
            throw new RegraDeNegocioException("Esta ocorrência já está marcada como resolvida.");
        }

        ocorrencia.setResolvida(true);
        ocorrencia.setResolvidoPor(responsavel);
        ocorrencia.setResolvidoEm(LocalDateTime.now());

        return ocorrenciaRepository.save(ocorrencia);
    }

    public List<Ocorrencia> filtrar(Long condominioId, LocalDate inicio, LocalDate fim, String tipo, Long unidadeId) {
        LocalDateTime inicioDateTime = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime fimDateTime = fim != null ? fim.atTime(LocalTime.MAX) : null;

        return ocorrenciaRepository.filtrarPorCondominio(condominioId, inicioDateTime, fimDateTime, tipo, unidadeId);
    }

    public List<Ocorrencia> listarPendentes(Long condominioId) {
        return ocorrenciaRepository.listarPendentesPorCondominio(condominioId);
    }

    private Ocorrencia buscarPorIdNoCondominio(Long condominioId, Long ocorrenciaId) {
        return ocorrenciaRepository.buscarPorIdNoCondominio(ocorrenciaId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Ocorrência não encontrada."));
    }

    private Unidade buscarUnidadeDoCondominioOuNulo(Long condominioId, Long unidadeId) {
        if (unidadeId == null) {
            return null;
        }

        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

        if (!unidade.getBloco().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Esta unidade não pertence a este condomínio.");
        }

        return unidade;
    }
}
