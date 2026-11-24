package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.OcorrenciaMorador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OcorrenciaMoradorRepository extends JpaRepository<OcorrenciaMorador, Long> {

    List<OcorrenciaMorador> findByUnidadeIdOrderByDataAberturaDesc(Long unidadeId);

    List<OcorrenciaMorador> findByUnidadeIdAndAbertoPorIdOrderByDataAberturaDesc(Long unidadeId, Long abertoPorId);

    List<OcorrenciaMorador> findByCondominioId(Long condominioId);

    Optional<OcorrenciaMorador> findByIdAndUnidadeId(Long id, Long unidadeId);

    Optional<OcorrenciaMorador> findByIdAndCondominioId(Long id, Long condominioId);
}
