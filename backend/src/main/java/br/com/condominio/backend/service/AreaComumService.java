package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.repository.AreaComumRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class AreaComumService {

    private final AreaComumRepository areaComumRepository;

    public AreaComumService(AreaComumRepository areaComumRepository) {
        this.areaComumRepository = areaComumRepository;
    }

    public AreaComum cadastrar(Condominio condominioJaValidado, String nome, Integer capacidade,
                               LocalTime horarioAbertura, LocalTime horarioFechamento, Boolean exigeAprovacao) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("Informe o nome da área comum.");
        }
        if (capacidade == null || capacidade <= 0) {
            throw new RegraDeNegocioException("A capacidade deve ser maior que zero.");
        }
        if (horarioAbertura == null || horarioFechamento == null) {
            throw new RegraDeNegocioException("Informe o horário de abertura e de fechamento da área comum.");
        }
        if (!horarioAbertura.isBefore(horarioFechamento)) {
            throw new RegraDeNegocioException("O horário de abertura deve ser anterior ao horário de fechamento.");
        }
        if (exigeAprovacao == null) {
            throw new RegraDeNegocioException("Informe se a área comum exige aprovação do síndico.");
        }
        if (areaComumRepository.existsByCondominioIdAndNomeIgnoreCase(condominioJaValidado.getId(), nome.trim())) {
            throw new RegraDeNegocioException("Já existe uma área comum com este nome neste condomínio.");
        }

        AreaComum area = new AreaComum();
        area.setCondominio(condominioJaValidado);
        area.setNome(nome.trim());
        area.setCapacidade(capacidade);
        area.setHorarioAbertura(horarioAbertura);
        area.setHorarioFechamento(horarioFechamento);
        area.setExigeAprovacao(exigeAprovacao);

        return areaComumRepository.save(area);
    }

    public List<AreaComum> listar(Long condominioId) {
        return areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId);
    }

    public AreaComum buscarNoCondominio(Long condominioId, Long areaComumId) {
        return areaComumRepository.findByIdAndCondominioId(areaComumId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Área comum não encontrada."));
    }
}
