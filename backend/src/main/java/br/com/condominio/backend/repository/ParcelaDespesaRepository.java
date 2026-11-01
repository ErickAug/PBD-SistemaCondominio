package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.ParcelaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ParcelaDespesaRepository extends JpaRepository<ParcelaDespesa, Long> {

    List<ParcelaDespesa> findByDespesaIdOrderByNumeroParcelaAsc(Long despesaId);

    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM ParcelaDespesa p
        WHERE p.despesa.condominio.id = :condominioId
        AND p.competencia = :competencia
        """)
    BigDecimal somarValorPorCompetencia(@Param("condominioId") Long condominioId,
                                        @Param("competencia") LocalDate competencia);

    void deleteByDespesaId(Long despesaId);
}