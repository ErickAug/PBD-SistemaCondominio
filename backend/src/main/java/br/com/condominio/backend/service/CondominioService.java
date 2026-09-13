package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CondominioService {

    private final CondominioRepository condominioRepository;
    private final AdministradoraRepository administradoraRepository;

    public CondominioService(CondominioRepository condominioRepository,
                             AdministradoraRepository administradoraRepository) {
        this.condominioRepository = condominioRepository;
        this.administradoraRepository = administradoraRepository;
    }

    public Condominio cadastrar(Condominio condominio, Long administradoraId) {
        Administradora administradora = administradoraRepository.findById(administradoraId)
                .orElseThrow(() -> new RegraDeNegocioException("Administradora não encontrada."));

        if (condominioRepository.existsByCnpj(condominio.getCnpj())) {
            throw new RegraDeNegocioException("Já existe um condomínio cadastrado com este CNPJ.");
        }

        condominio.setAdministradora(administradora);
        return condominioRepository.save(condominio);
    }

    public List<Condominio> listarPorAdministradora(Long administradoraId) {
        return condominioRepository.findByAdministradoraId(administradoraId);
    }
}