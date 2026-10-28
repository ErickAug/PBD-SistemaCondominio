package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.FechamentoMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FechamentoMensalRepository extends JpaRepository<FechamentoMensal, Long> {

    boolean existsByCondominioIdAndCompetencia(Long condominioId, LocalDate competencia);

    List<FechamentoMensal> findByCondominioIdOrderByCompetenciaDesc(Long condominioId);
}