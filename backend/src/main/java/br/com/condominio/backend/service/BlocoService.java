package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.repository.BlocoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlocoService {

    private final BlocoRepository blocoRepository;

    public BlocoService(BlocoRepository blocoRepository) {
        this.blocoRepository = blocoRepository;
    }

    public Bloco cadastrar(Bloco bloco, Condominio condominioJaValidado) {
        if (blocoRepository.existsByNomeAndCondominioId(bloco.getNome(), condominioJaValidado.getId())) {
            throw new RegraDeNegocioException("Já existe um bloco com este nome neste condomínio.");
        }

        bloco.setCondominio(condominioJaValidado);
        return blocoRepository.save(bloco);
    }

    public List<Bloco> listarPorCondominio(Long condominioId) {
        return blocoRepository.findByCondominioId(condominioId);
    }

    public Bloco buscarValidandoCondominio(Long blocoId, Long condominioId) {
        Bloco bloco = blocoRepository.findById(blocoId)
                .orElseThrow(() -> new RegraDeNegocioException("Bloco não encontrado."));

        if (!bloco.getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Este bloco não pertence a este condomínio.");
        }

        return bloco;
    }
}