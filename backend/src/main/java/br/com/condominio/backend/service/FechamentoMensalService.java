package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.FechamentoMensal;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.FechamentoMensalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FechamentoMensalService {
    private final RateioService rateioService;
    private final FechamentoMensalRepository fechamentoMensalRepository;

    public FechamentoMensalService(FechamentoMensalRepository fechamentoMensalRepository, RateioService rateioService) {
        this.fechamentoMensalRepository = fechamentoMensalRepository;
        this.rateioService = rateioService;
    }

    @Transactional
    public FechamentoMensal fechar(Condominio condominio, LocalDate competenciaInformada, Usuario usuario) {
        LocalDate competencia = competenciaInformada.withDayOfMonth(1);

        if (fechamentoMensalRepository.existsByCondominioIdAndCompetencia(condominio.getId(), competencia)) {
            throw new RegraDeNegocioException("Este mês já está fechado para este condomínio.");
        }

        FechamentoMensal fechamento = new FechamentoMensal();
        fechamento.setCondominio(condominio);
        fechamento.setCompetencia(competencia);
        fechamento.setDataFechamento(LocalDate.now());
        fechamento.setFechadoPor(usuario);
        fechamento = fechamentoMensalRepository.save(fechamento);

        rateioService.fechar(condominio, competencia, fechamento);

        return fechamento;
    }

    public boolean estaFechado(Long condominioId, LocalDate data) {
        LocalDate competencia = data.withDayOfMonth(1);
        return fechamentoMensalRepository.existsByCondominioIdAndCompetencia(condominioId, competencia);
    }

    public List<FechamentoMensal> listar(Long condominioId) {
        return fechamentoMensalRepository.findByCondominioIdOrderByCompetenciaDesc(condominioId);
    }
}