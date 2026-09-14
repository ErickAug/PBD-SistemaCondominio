package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.repository.BlocoRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlocoService {

    private final BlocoRepository blocoRepository;
    private final CondominioRepository condominioRepository;

    public BlocoService(BlocoRepository blocoRepository, CondominioRepository condominioRepository) {
        this.blocoRepository = blocoRepository;
        this.condominioRepository = condominioRepository;
    }

    public Bloco cadastrar(Bloco bloco, Long condominioId, Long administradoraIdAutenticado) {
        Condominio condominio = buscarCondominioValidandoTenant(condominioId, administradoraIdAutenticado);

        if (blocoRepository.existsByNomeAndCondominioId(bloco.getNome(), condominioId)) {
            throw new RegraDeNegocioException("Já existe um bloco com este nome neste condomínio.");
        }

        bloco.setCondominio(condominio);
        return blocoRepository.save(bloco);
    }

    public List<Bloco> listarPorCondominio(Long condominioId, Long administradoraIdAutenticado) {
        buscarCondominioValidandoTenant(condominioId, administradoraIdAutenticado);
        return blocoRepository.findByCondominioId(condominioId);
    }

    private Condominio buscarCondominioValidandoTenant(Long condominioId, Long administradoraIdAutenticado) {
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        if (!condominio.getAdministradora().getId().equals(administradoraIdAutenticado)) {
            throw new RegraDeNegocioException("Acesso negado a este condomínio.");
        }

        return condominio;
    }
}