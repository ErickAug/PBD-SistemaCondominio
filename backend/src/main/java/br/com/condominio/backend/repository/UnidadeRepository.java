package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    boolean existsByNumeroAndBlocoId(String numero, Long blocoId);

    List<Unidade> findByBlocoId(Long blocoId);

    List<Unidade> findByBlocoCondominioId(Long condominioId);

    @Query("""
            SELECT COALESCE(SUM(u.fracaoIdeal), 0)
            FROM Unidade u
            WHERE u.bloco.condominio.id = :condominioId
            """)
    BigDecimal somarFracaoIdealPorCondominio(@Param("condominioId") Long condominioId);
}