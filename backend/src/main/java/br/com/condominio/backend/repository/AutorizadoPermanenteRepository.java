package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.AutorizadoPermanente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AutorizadoPermanenteRepository extends JpaRepository<AutorizadoPermanente, Long> {

    List<AutorizadoPermanente> findByUnidadeId(Long unidadeId);

    List<AutorizadoPermanente> findByUnidadeIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            Long unidadeId, LocalDate hoje1, LocalDate hoje2);
}