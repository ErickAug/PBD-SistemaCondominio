package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Bloco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlocoRepository extends JpaRepository<Bloco, Long> {

    boolean existsByNomeAndCondominioId(String nome, Long condominioId);

    List<Bloco> findByCondominioId(Long condominioId);
}