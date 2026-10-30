package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Despesa;
import br.com.condominio.backend.model.enums.TipoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    @Query("""
            SELECT d FROM Despesa d
            WHERE d.condominio.id = :condominioId
            AND (:dataInicio IS NULL OR d.data >= :dataInicio)
            AND (:dataFim IS NULL OR d.data <= :dataFim)
            AND (:categoria IS NULL OR d.categoria = :categoria)
            AND (:tipo IS NULL OR d.tipo = :tipo)
            ORDER BY d.data DESC
            """)
    List<Despesa> filtrar(@Param("condominioId") Long condominioId,
                          @Param("dataInicio") LocalDate dataInicio,
                          @Param("dataFim") LocalDate dataFim,
                          @Param("categoria") String categoria,
                          @Param("tipo") TipoDespesa tipo);

    @Query("""
            SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d
            WHERE d.condominio.id = :condominioId
            AND (:dataInicio IS NULL OR d.data >= :dataInicio)
            AND (:dataFim IS NULL OR d.data <= :dataFim)
            AND (:categoria IS NULL OR d.categoria = :categoria)
            AND (:tipo IS NULL OR d.tipo = :tipo)
            """)
    BigDecimal somarValores(@Param("condominioId") Long condominioId,
                            @Param("dataInicio") LocalDate dataInicio,
                            @Param("dataFim") LocalDate dataFim,
                            @Param("categoria") String categoria,
                            @Param("tipo") TipoDespesa tipo);
}