package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.AutorizadoPermanente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AutorizadoPermanenteRepository extends JpaRepository<AutorizadoPermanente, Long> {

    List<AutorizadoPermanente> findByUnidadeId(Long unidadeId);

    List<AutorizadoPermanente> findByUnidadeIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            Long unidadeId, LocalDate hoje1, LocalDate hoje2);

    @Query("SELECT a FROM AutorizadoPermanente a WHERE a.unidade.bloco.condominio.id = :condominioId " +
            "AND (LOWER(a.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(a.documento) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<AutorizadoPermanente> buscarPorNomeOuDocumento(@Param("condominioId") Long condominioId,
                                                         @Param("termo") String termo);

    @Query("SELECT a FROM AutorizadoPermanente a WHERE a.unidade.bloco.condominio.id = :condominioId " +
            "AND :hoje BETWEEN a.dataInicio AND a.dataFim")
    List<AutorizadoPermanente> listarValidosHoje(@Param("condominioId") Long condominioId,
                                                  @Param("hoje") LocalDate hoje);
}
