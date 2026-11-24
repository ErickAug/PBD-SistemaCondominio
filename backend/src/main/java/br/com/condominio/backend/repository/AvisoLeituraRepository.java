package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.AvisoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvisoLeituraRepository extends JpaRepository<AvisoLeitura, Long> {

    List<AvisoLeitura> findByAvisoId(Long avisoId);

    Optional<AvisoLeitura> findByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);

    List<AvisoLeitura> findByUsuarioIdAndAvisoIdIn(Long usuarioId, Collection<Long> avisoIds);
}
