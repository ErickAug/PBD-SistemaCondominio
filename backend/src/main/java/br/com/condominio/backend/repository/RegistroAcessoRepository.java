package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.RegistroAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroAcessoRepository extends JpaRepository<RegistroAcesso, Long> {

    @Query("SELECT r FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "ORDER BY r.horaEntrada DESC")
    List<RegistroAcesso> listarPorCondominio(@Param("condominioId") Long condominioId);

    @Query("SELECT COUNT(r) > 0 FROM RegistroAcesso r WHERE r.unidade.bloco.condominio.id = :condominioId " +
            "AND r.documento = :documento AND r.horaSaida IS NULL")
    boolean existeEntradaAbertaPorDocumento(@Param("condominioId") Long condominioId,
                                             @Param("documento") String documento);
}
