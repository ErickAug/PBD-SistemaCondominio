package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CondominioRepository extends JpaRepository<Condominio, Long> {

    boolean existsByCnpj(String cnpj);

    List<Condominio> findByAdministradoraId(Long administradoraId);
}