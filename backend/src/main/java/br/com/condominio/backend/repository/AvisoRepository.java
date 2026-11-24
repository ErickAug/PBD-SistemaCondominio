package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findByCondominioIdOrderByDataCriacaoDesc(Long condominioId);

    Optional<Aviso> findByIdAndCondominioId(Long id, Long condominioId);

    @Query("""
            SELECT a FROM Aviso a
            LEFT JOIN a.bloco b
            LEFT JOIN a.unidade u
            WHERE a.condominio.id = :condominioId
            AND a.dataValidade >= :hoje
            AND (
                a.alcance = br.com.condominio.backend.model.enums.AlcanceAviso.CONDOMINIO
                OR (a.alcance = br.com.condominio.backend.model.enums.AlcanceAviso.BLOCO AND b.id IN :blocoIds)
                OR (a.alcance = br.com.condominio.backend.model.enums.AlcanceAviso.UNIDADE AND u.id IN :unidadeIds)
            )
            ORDER BY a.dataCriacao DESC
            """)
    List<Aviso> listarVigentesParaUnidades(@Param("condominioId") Long condominioId,
                                            @Param("hoje") LocalDate hoje,
                                            @Param("blocoIds") Collection<Long> blocoIds,
                                            @Param("unidadeIds") Collection<Long> unidadeIds);
}
