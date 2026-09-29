package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Dependente;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.repository.DependenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DependenteService {

    private final DependenteRepository dependenteRepository;

    public DependenteService(DependenteRepository dependenteRepository) {
        this.dependenteRepository = dependenteRepository;
    }

    public Dependente cadastrar(Unidade unidade, String nome) {
        Dependente dependente = new Dependente();
        dependente.setUnidade(unidade);
        dependente.setNome(nome);
        dependente.setAtivo(true);

        return dependenteRepository.save(dependente);
    }

    public void desativar(Long dependenteId, Long unidadeIdEsperada) {
        Dependente dependente = dependenteRepository.findById(dependenteId)
                .orElseThrow(() -> new RegraDeNegocioException("Dependente não encontrado."));

        if (!dependente.getUnidade().getId().equals(unidadeIdEsperada)) {
            throw new RegraDeNegocioException("Este dependente não pertence a esta unidade.");
        }

        dependente.setAtivo(false);
        dependenteRepository.save(dependente);
    }

    public void desativarTodosDaUnidade(Long unidadeId) {
        List<Dependente> ativos = dependenteRepository.findByUnidadeIdAndAtivoTrue(unidadeId);
        ativos.forEach(d -> d.setAtivo(false));
        dependenteRepository.saveAll(ativos);
    }

    public List<Dependente> listarAtivos(Long unidadeId) {
        return dependenteRepository.findByUnidadeIdAndAtivoTrue(unidadeId);
    }
}