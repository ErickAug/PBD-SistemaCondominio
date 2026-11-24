package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Reuniao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {

    List<Reuniao> findByCondominioIdOrderByDataDescHoraDesc(Long condominioId);

    Optional<Reuniao> findByIdAndCondominioId(Long id, Long condominioId);
}
