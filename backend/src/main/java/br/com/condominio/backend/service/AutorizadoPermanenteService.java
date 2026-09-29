package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AutorizadoPermanente;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.repository.AutorizadoPermanenteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AutorizadoPermanenteService {

    private final AutorizadoPermanenteRepository autorizadoPermanenteRepository;

    public AutorizadoPermanenteService(AutorizadoPermanenteRepository autorizadoPermanenteRepository) {
        this.autorizadoPermanenteRepository = autorizadoPermanenteRepository;
    }

    public AutorizadoPermanente cadastrar(Unidade unidade, String nome, String documento, String tipo,
                                          LocalDate dataInicio, LocalDate dataFim) {
        if (!dataFim.isAfter(dataInicio)) {
            throw new RegraDeNegocioException("A data de fim deve ser posterior à data de início.");
        }

        AutorizadoPermanente autorizado = new AutorizadoPermanente();
        autorizado.setUnidade(unidade);
        autorizado.setNome(nome);
        autorizado.setDocumento(documento);
        autorizado.setTipo(tipo);
        autorizado.setDataInicio(dataInicio);
        autorizado.setDataFim(dataFim);

        return autorizadoPermanenteRepository.save(autorizado);
    }

    public List<AutorizadoPermanente> listarPorUnidade(Long unidadeId) {
        return autorizadoPermanenteRepository.findByUnidadeId(unidadeId);
    }

    public boolean estaLiberadoHoje(AutorizadoPermanente autorizado) {
        LocalDate hoje = LocalDate.now();
        return !hoje.isBefore(autorizado.getDataInicio()) && !hoje.isAfter(autorizado.getDataFim());
    }
}