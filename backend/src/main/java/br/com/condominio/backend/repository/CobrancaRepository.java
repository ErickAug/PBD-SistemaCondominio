package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByRateioUnidadeId(Long rateioUnidadeId);

    @Query("""
            SELECT c FROM Cobranca c
            WHERE c.rateioUnidade.unidade.id = :unidadeId
            ORDER BY c.rateioUnidade.rateioMensal.competencia DESC
            """)
    List<Cobranca> findByUnidadeId(@Param("unidadeId") Long unidadeId);

    @Query("""
            SELECT c FROM Cobranca c
            WHERE c.rateioUnidade.rateioMensal.condominio.id = :condominioId
            AND (:competencia IS NULL OR c.rateioUnidade.rateioMensal.competencia = :competencia)
            AND (:situacao IS NULL OR c.situacao = :situacao)
            ORDER BY c.rateioUnidade.rateioMensal.competencia DESC, c.rateioUnidade.unidade.numero ASC
            """)
    List<Cobranca> filtrar(@Param("condominioId") Long condominioId,
                           @Param("competencia") LocalDate competencia,
                           @Param("situacao") SituacaoCobranca situacao);

    @Query("""
            SELECT c FROM Cobranca c
            WHERE c.rateioUnidade.rateioMensal.condominio.id = :condominioId
            AND c.situacao = br.com.condominio.backend.model.enums.SituacaoCobranca.ABERTA
            AND c.vencimento < :hoje
            ORDER BY c.vencimento ASC
            """)
    List<Cobranca> listarInadimplentes(@Param("condominioId") Long condominioId, @Param("hoje") LocalDate hoje);
}