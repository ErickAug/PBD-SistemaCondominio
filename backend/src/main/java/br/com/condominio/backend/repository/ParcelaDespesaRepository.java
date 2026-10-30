package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.ParcelaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParcelaDespesaRepository extends JpaRepository<ParcelaDespesa, Long> {

    List<ParcelaDespesa> findByDespesaIdOrderByNumeroParcelaAsc(Long despesaId);

    void deleteByDespesaId(Long despesaId);
}