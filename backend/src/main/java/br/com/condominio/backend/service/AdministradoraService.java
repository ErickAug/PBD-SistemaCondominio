package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.repository.AdministradoraRepository;
import org.springframework.stereotype.Service;

@Service
public class AdministradoraService {

    private final AdministradoraRepository administradoraRepository;

    public AdministradoraService(AdministradoraRepository administradoraRepository) {
        this.administradoraRepository = administradoraRepository;
    }

    public Administradora cadastrar(Administradora administradora) {
        if (administradoraRepository.existsByCnpj(administradora.getCnpj())) {
            throw new RegraDeNegocioException("Já existe uma administradora cadastrada com este CNPJ.");
        }

        return administradoraRepository.save(administradora);
    }
}