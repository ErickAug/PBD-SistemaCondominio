package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.OcorrenciaMoradorHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OcorrenciaMoradorHistoricoRepository extends JpaRepository<OcorrenciaMoradorHistorico, Long> {

    List<OcorrenciaMoradorHistorico> findByOcorrenciaIdOrderByDataHoraAscIdAsc(Long ocorrenciaId);
}
