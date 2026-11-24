package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import br.com.condominio.backend.model.enums.SituacaoReserva;
import br.com.condominio.backend.repository.AreaComumRepository;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    public static final Set<SituacaoReserva> SITUACOES_ATIVAS =
            Set.of(SituacaoReserva.PENDENTE, SituacaoReserva.CONFIRMADA);

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaComumRepository;
    private final CobrancaRepository cobrancaRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          AreaComumRepository areaComumRepository,
                          CobrancaRepository cobrancaRepository) {
        this.reservaRepository = reservaRepository;
        this.areaComumRepository = areaComumRepository;
        this.cobrancaRepository = cobrancaRepository;
    }

    public Reserva reservar(Unidade unidadeJaValidada, Usuario autor, Long areaComumId, LocalDate data,
                            LocalTime horaInicio, LocalTime horaFim, String finalidade) {
        if (areaComumId == null) {
            throw new RegraDeNegocioException("Informe a área comum.");
        }

        Long condominioId = unidadeJaValidada.getBloco().getCondominio().getId();
        AreaComum area = areaComumRepository.findByIdAndCondominioId(areaComumId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Área comum não encontrada."));

        if (data == null) {
            throw new RegraDeNegocioException("Informe a data da reserva.");
        }
        if (horaInicio == null || horaFim == null) {
            throw new RegraDeNegocioException("Informe o horário de início e de fim da reserva.");
        }
        if (!horaInicio.isBefore(horaFim)) {
            throw new RegraDeNegocioException("O horário de início deve ser anterior ao horário de fim.");
        }
        if (finalidade == null || finalidade.isBlank()) {
            throw new RegraDeNegocioException("Informe a finalidade da reserva.");
        }
        if (data.isBefore(LocalDate.now())) {
            throw new RegraDeNegocioException("Não é possível reservar uma data que já passou.");
        }
        if (horaInicio.isBefore(area.getHorarioAbertura()) || horaFim.isAfter(area.getHorarioFechamento())) {
            throw new RegraDeNegocioException("A reserva precisa estar dentro do horário de funcionamento da área: das "
                    + area.getHorarioAbertura().format(HORA) + " às " + area.getHorarioFechamento().format(HORA) + ".");
        }

        validarAdimplencia(unidadeJaValidada);
        validarConflito(area, data, horaInicio, horaFim);

        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setUnidade(unidadeJaValidada);
        reserva.setData(data);
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFim(horaFim);
        reserva.setFinalidade(finalidade.trim());
        reserva.setSituacao(area.isExigeAprovacao() ? SituacaoReserva.PENDENTE : SituacaoReserva.CONFIRMADA);
        reserva.setCriadoPor(autor);
        reserva.setDataCriacao(agora());

        return reservaRepository.save(reserva);
    }

    public List<Reserva> listarDaUnidade(Long unidadeId) {
        return reservaRepository.findByUnidadeIdOrderByDataDescHoraInicioDesc(unidadeId);
    }

    public Reserva cancelar(Long unidadeId, Long reservaId, Usuario usuario) {
        Reserva reserva = reservaRepository.findByIdAndUnidadeId(reservaId, unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Reserva não encontrada."));

        if (!SITUACOES_ATIVAS.contains(reserva.getSituacao())) {
            throw new RegraDeNegocioException("Esta reserva já foi cancelada ou recusada.");
        }
        if (!reserva.getData().isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("Só é possível cancelar a reserva antes do dia marcado.");
        }

        reserva.setSituacao(SituacaoReserva.CANCELADA);
        reserva.setCanceladoPor(usuario);
        reserva.setDataCancelamento(agora());

        return reservaRepository.save(reserva);
    }

    public List<Reserva> listarPendentes(Long condominioId) {
        return reservaRepository.findByAreaComumCondominioIdAndSituacaoOrderByDataAscHoraInicioAsc(
                condominioId, SituacaoReserva.PENDENTE);
    }

    public Reserva aprovar(Long condominioId, Long reservaId, Usuario sindico) {
        Reserva reserva = buscarPendente(condominioId, reservaId, "aprovar");

        reserva.setSituacao(SituacaoReserva.CONFIRMADA);
        reserva.setDecididoPor(sindico);
        reserva.setDataDecisao(agora());

        return reservaRepository.save(reserva);
    }

    public Reserva recusar(Long condominioId, Long reservaId, Usuario sindico, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("Informe o motivo da recusa.");
        }

        Reserva reserva = buscarPendente(condominioId, reservaId, "recusar");

        reserva.setSituacao(SituacaoReserva.RECUSADA);
        reserva.setDecididoPor(sindico);
        reserva.setDataDecisao(agora());
        reserva.setMotivoRecusa(motivo.trim());

        return reservaRepository.save(reserva);
    }

    public Map<AreaComum, List<Reserva>> montarCalendario(Long condominioId, String mes, Long areaComumId) {
        YearMonth competencia = interpretarMes(mes);

        List<AreaComum> areas = areaComumId != null
                ? List.of(areaComumRepository.findByIdAndCondominioId(areaComumId, condominioId)
                        .orElseThrow(() -> new RegraDeNegocioException("Área comum não encontrada.")))
                : areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId);

        Map<Long, List<Reserva>> reservasPorArea = reservaRepository
                .findByAreaComumCondominioIdAndDataBetweenAndSituacaoInOrderByDataAscHoraInicioAsc(
                        condominioId, competencia.atDay(1), competencia.atEndOfMonth(), SITUACOES_ATIVAS)
                .stream()
                .sorted(Comparator.comparing(Reserva::getData).thenComparing(Reserva::getHoraInicio))
                .collect(Collectors.groupingBy(r -> r.getAreaComum().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<AreaComum, List<Reserva>> calendario = new LinkedHashMap<>();
        areas.forEach(a -> calendario.put(a, reservasPorArea.getOrDefault(a.getId(), List.of())));
        return calendario;
    }

    private Reserva buscarPendente(Long condominioId, Long reservaId, String acao) {
        Reserva reserva = reservaRepository.findByIdAndAreaComumCondominioId(reservaId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Reserva não encontrada."));

        if (reserva.getSituacao() != SituacaoReserva.PENDENTE) {
            throw new RegraDeNegocioException("Só é possível " + acao + " uma reserva pendente.");
        }

        return reserva;
    }

    private void validarAdimplencia(Unidade unidade) {
        LocalDate hoje = LocalDate.now();

        List<Cobranca> emAtraso = cobrancaRepository.findByUnidadeId(unidade.getId()).stream()
                .filter(c -> c.getSituacao() == SituacaoCobranca.ABERTA && c.getVencimento().isBefore(hoje))
                .sorted(Comparator.comparing(Cobranca::getVencimento))
                .toList();

        if (emAtraso.isEmpty()) {
            return;
        }

        String quantidade = emAtraso.size() == 1
                ? "1 cobrança em atraso"
                : emAtraso.size() + " cobranças em atraso";

        throw new RegraDeNegocioException("A unidade está inadimplente e não pode reservar: " + quantidade
                + ", a mais antiga vencida em " + emAtraso.get(0).getVencimento().format(DATA)
                + ". Regularize o pagamento para reservar.");
    }

    private void validarConflito(AreaComum area, LocalDate data, LocalTime inicio, LocalTime fim) {
        reservaRepository.findByAreaComumIdAndDataAndSituacaoIn(area.getId(), data, SITUACOES_ATIVAS).stream()
                .filter(r -> inicio.isBefore(r.getHoraFim()) && r.getHoraInicio().isBefore(fim))
                .min(Comparator.comparing(Reserva::getHoraInicio))
                .ifPresent(r -> {
                    throw new RegraDeNegocioException("Horário indisponível: conflita com a reserva #" + r.getId()
                            + " de " + area.getNome() + " em " + r.getData().format(DATA)
                            + ", das " + r.getHoraInicio().format(HORA) + " às " + r.getHoraFim().format(HORA)
                            + " (bloco " + r.getUnidade().getBloco().getNome()
                            + ", unidade " + r.getUnidade().getNumero()
                            + ", " + r.getSituacao().name().toLowerCase() + ").");
                });
    }

    private YearMonth interpretarMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException e) {
            throw new RegraDeNegocioException("Informe o mês no formato AAAA-MM.");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
