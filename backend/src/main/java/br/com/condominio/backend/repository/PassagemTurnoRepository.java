package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.PassagemTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassagemTurnoRepository extends JpaRepository<PassagemTurno, Long> {

    Optional<PassagemTurno> findTopByCondominioIdOrderByHorarioEncerramentoDesc(Long condominioId);
}
