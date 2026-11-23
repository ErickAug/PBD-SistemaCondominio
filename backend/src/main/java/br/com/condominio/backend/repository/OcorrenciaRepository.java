package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    @Query("SELECT o FROM Ocorrencia o WHERE o.id = :id AND o.condominio.id = :condominioId")
    Optional<Ocorrencia> buscarPorIdNoCondominio(@Param("id") Long id,
                                                  @Param("condominioId") Long condominioId);

    @Query("SELECT o FROM Ocorrencia o WHERE o.condominio.id = :condominioId " +
            "AND (:inicio IS NULL OR o.dataHora >= :inicio) " +
            "AND (:fim IS NULL OR o.dataHora <= :fim) " +
            "AND (:tipo IS NULL OR o.tipo = :tipo) " +
            "AND (:unidadeId IS NULL OR o.unidade.id = :unidadeId) " +
            "ORDER BY o.dataHora DESC")
    List<Ocorrencia> filtrarPorCondominio(@Param("condominioId") Long condominioId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim,
                                           @Param("tipo") String tipo,
                                           @Param("unidadeId") Long unidadeId);

    @Query("SELECT o FROM Ocorrencia o WHERE o.condominio.id = :condominioId " +
            "AND o.resolvida = false ORDER BY o.dataHora ASC")
    List<Ocorrencia> listarPendentesPorCondominio(@Param("condominioId") Long condominioId);
}
