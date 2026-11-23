package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.model.PassagemTurno;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.OcorrenciaRepository;
import br.com.condominio.backend.repository.PassagemTurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PassagemTurnoService {

    private final PassagemTurnoRepository passagemTurnoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final CondominioRepository condominioRepository;

    public PassagemTurnoService(PassagemTurnoRepository passagemTurnoRepository,
                                 OcorrenciaRepository ocorrenciaRepository,
                                 CondominioRepository condominioRepository) {
        this.passagemTurnoRepository = passagemTurnoRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.condominioRepository = condominioRepository;
    }

    public PassagemTurno encerrarTurno(Long condominioId, Usuario porteiro) {
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        List<Ocorrencia> pendencias = ocorrenciaRepository.listarPendentesPorCondominio(condominioId);

        PassagemTurno passagemTurno = new PassagemTurno();
        passagemTurno.setCondominio(condominio);
        passagemTurno.setPorteiro(porteiro);
        passagemTurno.setHorarioEncerramento(LocalDateTime.now());
        passagemTurno.setPendencias(pendencias);

        return passagemTurnoRepository.save(passagemTurno);
    }

    public Optional<PassagemTurno> buscarUltimoTurno(Long condominioId) {
        return passagemTurnoRepository.findTopByCondominioIdOrderByHorarioEncerramentoDesc(condominioId);
    }
}
