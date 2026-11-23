package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.RegistroAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistroAcessoRepository extends JpaRepository<RegistroAcesso, Long> {

    @Query("SELECT r FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "ORDER BY r.horaEntrada DESC")
    List<RegistroAcesso> listarPorCondominio(@Param("condominioId") Long condominioId);

    @Query("SELECT COUNT(r) > 0 FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "AND r.documento = :documento AND r.horaSaida IS NULL")
    boolean existeEntradaAbertaPorDocumento(@Param("condominioId") Long condominioId,
                                             @Param("documento") String documento);

    @Query("SELECT r FROM RegistroAcesso r WHERE r.id = :id AND r.unidade.bloco.condominio.id = :condominioId")
    Optional<RegistroAcesso> buscarPorIdNoCondominio(@Param("id") Long id,
                                                      @Param("condominioId") Long condominioId);

    @Query("SELECT r FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "AND r.horaSaida IS NULL ORDER BY r.horaEntrada ASC")
    List<RegistroAcesso> listarAbertosPorCondominio(@Param("condominioId") Long condominioId);

    @Query("SELECT r FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "AND r.unidade.id = :unidadeId AND r.horaSaida IS NULL ORDER BY r.horaEntrada ASC")
    List<RegistroAcesso> listarAbertosPorUnidade(@Param("condominioId") Long condominioId,
                                                  @Param("unidadeId") Long unidadeId);

    @Query("SELECT r FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "AND (:inicio IS NULL OR r.horaEntrada >= :inicio) " +
            "AND (:fim IS NULL OR r.horaEntrada <= :fim) " +
            "AND (:unidadeId IS NULL OR r.unidade.id = :unidadeId) " +
            "AND (:tipo IS NULL OR r.tipo = :tipo) " +
            "ORDER BY r.horaEntrada DESC")
    List<RegistroAcesso> filtrarHistorico(@Param("condominioId") Long condominioId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim,
                                           @Param("unidadeId") Long unidadeId,
                                           @Param("tipo") String tipo);
}
