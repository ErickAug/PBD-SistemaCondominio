package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.VinculoMorador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VinculoMoradorRepository extends JpaRepository<VinculoMorador, Long> {

    List<VinculoMorador> findByUnidadeIdAndDataSaidaIsNull(Long unidadeId);

    List<VinculoMorador> findByUnidadeIdOrderByDataEntradaDesc(Long unidadeId);

    List<VinculoMorador> findByMoradorIdAndDataSaidaIsNull(Long moradorId);
}