package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Dependente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {

    List<Dependente> findByUnidadeIdAndAtivoTrue(Long unidadeId);

    List<Dependente> findByUnidadeId(Long unidadeId);
}