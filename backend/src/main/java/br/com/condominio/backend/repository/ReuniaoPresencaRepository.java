package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.ReuniaoPresenca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReuniaoPresencaRepository extends JpaRepository<ReuniaoPresenca, Long> {

    List<ReuniaoPresenca> findByReuniaoId(Long reuniaoId);

    Optional<ReuniaoPresenca> findByReuniaoIdAndUnidadeId(Long reuniaoId, Long unidadeId);

    long countByReuniaoId(Long reuniaoId);
}
