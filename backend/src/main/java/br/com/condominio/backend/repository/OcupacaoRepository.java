package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Ocupacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OcupacaoRepository extends JpaRepository<Ocupacao, Long> {

    Optional<Ocupacao> findFirstByUnidadeIdOrderByDataEntradaDesc(Long unidadeId);

    Optional<Ocupacao> findFirstByUnidadeIdAndDataEntradaLessThanEqualOrderByDataEntradaDesc(
            Long unidadeId, LocalDate data);

    List<Ocupacao> findByUnidadeIdOrderByDataEntradaDesc(Long unidadeId);

    boolean existsByUnidadeId(Long unidadeId);

    @Query("""
            SELECT o FROM Ocupacao o
            WHERE o.unidade.id IN :unidadeIds
            AND o.dataEntrada = (
                SELECT MAX(o2.dataEntrada) FROM Ocupacao o2 WHERE o2.unidade.id = o.unidade.id
            )
            """)
    List<Ocupacao> buscarOcupacoesAtuais(@Param("unidadeIds") List<Long> unidadeIds);
}