package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    boolean existsByNumeroAndCondominioId(String numero, Long condominioId);

    List<Vaga> findByUnidadeIdOrderByNumeroAsc(Long unidadeId);

    long countByUnidadeId(Long unidadeId);
}