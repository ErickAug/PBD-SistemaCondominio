package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.AutorizacaoVisita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutorizacaoVisitaRepository extends JpaRepository<AutorizacaoVisita, Long> {

    List<AutorizacaoVisita> findByUnidadeId(Long unidadeId);

    Optional<AutorizacaoVisita> findByIdAndUnidadeId(Long id, Long unidadeId);

    @Query("SELECT a FROM AutorizacaoVisita a WHERE a.unidade.bloco.condominio.id = :condominioId " +
            "AND (LOWER(a.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(a.documento) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<AutorizacaoVisita> buscarPorNomeOuDocumento(@Param("condominioId") Long condominioId,
                                                      @Param("termo") String termo);

    @Query("SELECT a FROM AutorizacaoVisita a WHERE a.unidade.bloco.condominio.id = :condominioId " +
            "AND a.status = br.com.condominio.backend.model.enums.StatusAutorizacaoVisita.PENDENTE " +
            "AND :hoje BETWEEN a.dataInicio AND a.dataFim")
    List<AutorizacaoVisita> listarEsperadasHoje(@Param("condominioId") Long condominioId,
                                                 @Param("hoje") LocalDate hoje);
}
