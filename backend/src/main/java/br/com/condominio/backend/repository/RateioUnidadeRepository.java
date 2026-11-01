package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.RateioUnidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateioUnidadeRepository extends JpaRepository<RateioUnidade, Long> {

    List<RateioUnidade> findByRateioMensalIdOrderByUnidadeNumeroAsc(Long rateioMensalId);
}