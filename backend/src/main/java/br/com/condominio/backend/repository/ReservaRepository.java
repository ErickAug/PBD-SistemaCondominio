package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.enums.SituacaoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByAreaComumIdAndDataAndSituacaoIn(Long areaComumId, LocalDate data,
                                                       Collection<SituacaoReserva> situacoes);

    List<Reserva> findByUnidadeIdOrderByDataDescHoraInicioDesc(Long unidadeId);

    Optional<Reserva> findByIdAndUnidadeId(Long id, Long unidadeId);

    Optional<Reserva> findByIdAndAreaComumCondominioId(Long id, Long condominioId);

    List<Reserva> findByAreaComumCondominioIdAndSituacaoOrderByDataAscHoraInicioAsc(Long condominioId,
                                                                                   SituacaoReserva situacao);

    List<Reserva> findByAreaComumCondominioIdAndDataBetweenAndSituacaoInOrderByDataAscHoraInicioAsc(
            Long condominioId, LocalDate inicio, LocalDate fim, Collection<SituacaoReserva> situacoes);
}
