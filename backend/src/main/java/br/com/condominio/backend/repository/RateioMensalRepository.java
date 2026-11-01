package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.RateioMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RateioMensalRepository extends JpaRepository<RateioMensal, Long> {

    boolean existsByCondominioIdAndCompetencia(Long condominioId, LocalDate competencia);

    Optional<RateioMensal> findByCondominioIdAndCompetencia(Long condominioId, LocalDate competencia);
}