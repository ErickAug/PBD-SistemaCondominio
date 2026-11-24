package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.AreaComum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreaComumRepository extends JpaRepository<AreaComum, Long> {

    List<AreaComum> findByCondominioIdOrderByNomeAsc(Long condominioId);

    boolean existsByCondominioIdAndNomeIgnoreCase(Long condominioId, String nome);

    Optional<AreaComum> findByIdAndCondominioId(Long id, Long condominioId);
}
