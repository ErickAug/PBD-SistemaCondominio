package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Administradora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradoraRepository extends JpaRepository<Administradora, Long> {

    boolean existsByCnpj(String cnpj);
}