package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.ReuniaoPautaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReuniaoPautaItemRepository extends JpaRepository<ReuniaoPautaItem, Long> {

    List<ReuniaoPautaItem> findByReuniaoIdOrderByOrdemAsc(Long reuniaoId);

    long countByReuniaoId(Long reuniaoId);
}
