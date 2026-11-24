package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import br.com.condominio.backend.model.enums.SituacaoReserva;
import br.com.condominio.backend.repository.AreaComumRepository;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final Long CONDOMINIO_ID = 1L;
    private static final LocalDate AMANHA = LocalDate.now().plusDays(1);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private AreaComumRepository areaComumRepository;
    @Mock
    private CobrancaRepository cobrancaRepository;

    @InjectMocks
    private ReservaService service;

    private Unidade unidade;
    private Unidade vizinha;
    private AreaComum salao;
    private AreaComum churrasqueira;
    private Usuario morador;
    private Usuario sindico;

    @BeforeEach
    void setUp() {
        Condominio condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);

        Bloco bloco = new Bloco();
        bloco.setId(10L);
        bloco.setNome("A");
        bloco.setCondominio(condominio);

        unidade = unidade(101L, "101", bloco);
        vizinha = unidade(102L, "102", bloco);

        salao = area(20L, "Salao de festas", condominio, true);
        churrasqueira = area(21L, "Churrasqueira", condominio, false);

        morador = new Usuario();
        morador.setId(5L);
        morador.setNome("Morador");
        morador.setPerfil(Perfil.MORADOR);

        sindico = new Usuario();
        sindico.setId(6L);
        sindico.setNome("Sindico");
        sindico.setPerfil(Perfil.SINDICO);

        lenient().when(areaComumRepository.findByIdAndCondominioId(20L, CONDOMINIO_ID)).thenReturn(Optional.of(salao));
        lenient().when(areaComumRepository.findByIdAndCondominioId(21L, CONDOMINIO_ID)).thenReturn(Optional.of(churrasqueira));
        lenient().when(cobrancaRepository.findByUnidadeId(anyLong())).thenReturn(List.of());
        lenient().when(reservaRepository.findByAreaComumIdAndDataAndSituacaoIn(anyLong(), any(), anyCollection()))
                .thenReturn(List.of());
        lenient().when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reservaGuardaDataFaixaUnidadeEFinalidade() {
        Reserva r = service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(16, 0),
                " Aniversario ");

        assertSame(churrasqueira, r.getAreaComum());
        assertSame(unidade, r.getUnidade());
        assertEquals(AMANHA, r.getData());
        assertEquals(LocalTime.of(12, 0), r.getHoraInicio());
        assertEquals(LocalTime.of(16, 0), r.getHoraFim());
        assertEquals("Aniversario", r.getFinalidade());
        assertSame(morador, r.getCriadoPor());
        assertNotNull(r.getDataCriacao());
    }

    @Test
    void areaSemAprovacaoJaNasceConfirmada() {
        Reserva r = service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(16, 0), "Almoco");

        assertEquals(SituacaoReserva.CONFIRMADA, r.getSituacao());
    }

    @Test
    void areaQueExigeAprovacaoNascePendente() {
        Reserva r = service.reservar(unidade, morador, 20L, AMANHA, LocalTime.of(18, 0), LocalTime.of(22, 0), "Festa");

        assertEquals(SituacaoReserva.PENDENTE, r.getSituacao());
    }

    @Test
    void areaDeOutroCondominioNaoEhEncontrada() {
        when(areaComumRepository.findByIdAndCondominioId(99L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 99L, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0), "F"));

        assertEquals("Área comum não encontrada.", erro.getMessage());
    }

    @Test
    void semFinalidadeEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0), " "));

        assertEquals("Informe a finalidade da reserva.", erro.getMessage());
    }

    @Test
    void semDataEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, null, LocalTime.of(12, 0), LocalTime.of(14, 0), "F"));

        assertEquals("Informe a data da reserva.", erro.getMessage());
    }

    @Test
    void inicioDepoisDoFimEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(16, 0), LocalTime.of(12, 0), "F"));

        assertEquals("O horário de início deve ser anterior ao horário de fim.", erro.getMessage());
    }

    @Test
    void dataNoPassadoEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, LocalDate.now().minusDays(1), LocalTime.of(12, 0),
                        LocalTime.of(14, 0), "F"));

        assertEquals("Não é possível reservar uma data que já passou.", erro.getMessage());
    }

    @Test
    void reservaQueComecaAntesDaAberturaEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(7, 0), LocalTime.of(10, 0), "F"));

        assertEquals("A reserva precisa estar dentro do horário de funcionamento da área: das 08:00 às 22:00.",
                erro.getMessage());
    }

    @Test
    void reservaQueTerminaDepoisDoFechamentoEhRecusada() {
        assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(20, 0), LocalTime.of(23, 0), "F"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservaQueOcupaOHorarioInteiroDeFuncionamentoEhAceita() {
        Reserva r = service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(8, 0), LocalTime.of(22, 0), "Evento");

        assertEquals(SituacaoReserva.CONFIRMADA, r.getSituacao());
    }

    @Test
    void unidadeInadimplenteNaoReservaEOSistemaDizOMotivo() {
        LocalDate vencida = LocalDate.now().minusDays(40);
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(SituacaoCobranca.ABERTA, LocalDate.now().minusDays(10)),
                cobranca(SituacaoCobranca.ABERTA, vencida),
                cobranca(SituacaoCobranca.PAGA, LocalDate.now().minusDays(70))));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0), "F"));

        assertEquals("A unidade está inadimplente e não pode reservar: 2 cobranças em atraso, a mais antiga vencida em "
                + vencida.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ". Regularize o pagamento para reservar.",
                erro.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cobrancaAbertaAindaNoPrazoNaoImpedeReserva() {
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(SituacaoCobranca.ABERTA, LocalDate.now().plusDays(5)),
                cobranca(SituacaoCobranca.ABERTA, LocalDate.now())));

        Reserva r = service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0), "F");

        assertNotNull(r);
    }

    @Test
    void reservaQueConflitaComOutraDoMesmoEspacoEhRecusadaDizendoQual() {
        Reserva existente = reserva(77L, vizinha, churrasqueira, AMANHA, LocalTime.of(12, 0), LocalTime.of(16, 0),
                SituacaoReserva.CONFIRMADA);
        when(reservaRepository.findByAreaComumIdAndDataAndSituacaoIn(eq(21L), eq(AMANHA), anyCollection()))
                .thenReturn(List.of(existente));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(15, 0), LocalTime.of(18, 0), "F"));

        assertEquals("Horário indisponível: conflita com a reserva #77 de Churrasqueira em "
                + AMANHA.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ", das 12:00 às 16:00 (bloco A, unidade 102, confirmada).", erro.getMessage());
    }

    @Test
    void reservaPendenteTambemBloqueiaOHorario() {
        Reserva pendente = reserva(78L, vizinha, salao, AMANHA, LocalTime.of(18, 0), LocalTime.of(22, 0),
                SituacaoReserva.PENDENTE);
        when(reservaRepository.findByAreaComumIdAndDataAndSituacaoIn(eq(20L), eq(AMANHA), anyCollection()))
                .thenReturn(List.of(pendente));

        assertThrows(RegraDeNegocioException.class,
                () -> service.reservar(unidade, morador, 20L, AMANHA, LocalTime.of(19, 0), LocalTime.of(20, 0), "F"));
    }

    @Test
    void reservaQueEncostaNoFimDaOutraNaoConflita() {
        Reserva existente = reserva(77L, vizinha, churrasqueira, AMANHA, LocalTime.of(12, 0), LocalTime.of(16, 0),
                SituacaoReserva.CONFIRMADA);
        when(reservaRepository.findByAreaComumIdAndDataAndSituacaoIn(eq(21L), eq(AMANHA), anyCollection()))
                .thenReturn(List.of(existente));

        Reserva r = service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(16, 0), LocalTime.of(18, 0), "F");

        assertNotNull(r);
    }

    @Test
    void conflitoSoConsideraReservasAtivas() {
        service.reservar(unidade, morador, 21L, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0), "F");

        verify(reservaRepository).findByAreaComumIdAndDataAndSituacaoIn(21L, AMANHA, ReservaService.SITUACOES_ATIVAS);
    }

    @Test
    void sindicoAprovaReservaPendente() {
        Reserva pendente = reserva(1L, unidade, salao, AMANHA, LocalTime.of(18, 0), LocalTime.of(22, 0),
                SituacaoReserva.PENDENTE);
        when(reservaRepository.findByIdAndAreaComumCondominioId(1L, CONDOMINIO_ID)).thenReturn(Optional.of(pendente));

        Reserva aprovada = service.aprovar(CONDOMINIO_ID, 1L, sindico);

        assertEquals(SituacaoReserva.CONFIRMADA, aprovada.getSituacao());
        assertSame(sindico, aprovada.getDecididoPor());
        assertNotNull(aprovada.getDataDecisao());
    }

    @Test
    void naoAprovaReservaQueNaoEstaPendente() {
        Reserva confirmada = reserva(1L, unidade, churrasqueira, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0),
                SituacaoReserva.CONFIRMADA);
        when(reservaRepository.findByIdAndAreaComumCondominioId(1L, CONDOMINIO_ID)).thenReturn(Optional.of(confirmada));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.aprovar(CONDOMINIO_ID, 1L, sindico));

        assertEquals("Só é possível aprovar uma reserva pendente.", erro.getMessage());
    }

    @Test
    void sindicoRecusaComMotivo() {
        Reserva pendente = reserva(1L, unidade, salao, AMANHA, LocalTime.of(18, 0), LocalTime.of(22, 0),
                SituacaoReserva.PENDENTE);
        when(reservaRepository.findByIdAndAreaComumCondominioId(1L, CONDOMINIO_ID)).thenReturn(Optional.of(pendente));

        Reserva recusada = service.recusar(CONDOMINIO_ID, 1L, sindico, " Manutencao no salao ");

        assertEquals(SituacaoReserva.RECUSADA, recusada.getSituacao());
        assertEquals("Manutencao no salao", recusada.getMotivoRecusa());
    }

    @Test
    void recusarSemMotivoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.recusar(CONDOMINIO_ID, 1L, sindico, " "));

        assertEquals("Informe o motivo da recusa.", erro.getMessage());
    }

    @Test
    void reservaDeOutroCondominioNaoEhEncontradaPeloSindico() {
        when(reservaRepository.findByIdAndAreaComumCondominioId(1L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.aprovar(CONDOMINIO_ID, 1L, sindico));

        assertEquals("Reserva não encontrada.", erro.getMessage());
    }

    @Test
    void moradorCancelaAPropriaReservaAntesDaData() {
        Reserva r = reserva(1L, unidade, churrasqueira, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0),
                SituacaoReserva.CONFIRMADA);
        when(reservaRepository.findByIdAndUnidadeId(1L, 101L)).thenReturn(Optional.of(r));

        Reserva cancelada = service.cancelar(101L, 1L, morador);

        assertEquals(SituacaoReserva.CANCELADA, cancelada.getSituacao());
        assertSame(morador, cancelada.getCanceladoPor());
        assertNotNull(cancelada.getDataCancelamento());
    }

    @Test
    void naoCancelaNoProprioDia() {
        Reserva r = reserva(1L, unidade, churrasqueira, LocalDate.now(), LocalTime.of(20, 0), LocalTime.of(22, 0),
                SituacaoReserva.CONFIRMADA);
        when(reservaRepository.findByIdAndUnidadeId(1L, 101L)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cancelar(101L, 1L, morador));

        assertEquals("Só é possível cancelar a reserva antes do dia marcado.", erro.getMessage());
    }

    @Test
    void naoCancelaReservaDeOutraUnidade() {
        when(reservaRepository.findByIdAndUnidadeId(1L, 101L)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cancelar(101L, 1L, morador));

        assertEquals("Reserva não encontrada.", erro.getMessage());
    }

    @Test
    void naoCancelaDuasVezes() {
        Reserva r = reserva(1L, unidade, churrasqueira, AMANHA, LocalTime.of(12, 0), LocalTime.of(14, 0),
                SituacaoReserva.CANCELADA);
        when(reservaRepository.findByIdAndUnidadeId(1L, 101L)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cancelar(101L, 1L, morador));

        assertEquals("Esta reserva já foi cancelada ou recusada.", erro.getMessage());
    }

    @Test
    void calendarioMostraAsReservasDoMesPorArea() {
        YearMonth mes = YearMonth.now().plusMonths(1);
        Reserva noSalao = reserva(1L, unidade, salao, mes.atDay(5), LocalTime.of(18, 0), LocalTime.of(22, 0),
                SituacaoReserva.PENDENTE);
        Reserva naChurrasqueira = reserva(2L, vizinha, churrasqueira, mes.atDay(3), LocalTime.of(12, 0),
                LocalTime.of(14, 0), SituacaoReserva.CONFIRMADA);
        when(areaComumRepository.findByCondominioIdOrderByNomeAsc(CONDOMINIO_ID)).thenReturn(List.of(churrasqueira, salao));
        when(reservaRepository.findByAreaComumCondominioIdAndDataBetweenAndSituacaoInOrderByDataAscHoraInicioAsc(
                CONDOMINIO_ID, mes.atDay(1), mes.atEndOfMonth(), ReservaService.SITUACOES_ATIVAS))
                .thenReturn(List.of(naChurrasqueira, noSalao));

        Map<AreaComum, List<Reserva>> calendario = service.montarCalendario(CONDOMINIO_ID, mes.toString(), null);

        assertEquals(List.of(churrasqueira, salao), List.copyOf(calendario.keySet()));
        assertEquals(List.of(naChurrasqueira), calendario.get(churrasqueira));
        assertEquals(List.of(noSalao), calendario.get(salao));
    }

    @Test
    void calendarioDeUmaAreaSoMostraEla() {
        YearMonth mes = YearMonth.now();
        when(reservaRepository.findByAreaComumCondominioIdAndDataBetweenAndSituacaoInOrderByDataAscHoraInicioAsc(
                eq(CONDOMINIO_ID), any(), any(), anyCollection())).thenReturn(List.of());

        Map<AreaComum, List<Reserva>> calendario = service.montarCalendario(CONDOMINIO_ID, mes.toString(), 20L);

        assertEquals(List.of(salao), List.copyOf(calendario.keySet()));
        assertTrue(calendario.get(salao).isEmpty());
    }

    @Test
    void calendarioComMesInvalidoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.montarCalendario(CONDOMINIO_ID, "dezembro", null));

        assertEquals("Informe o mês no formato AAAA-MM.", erro.getMessage());
    }

    private static Unidade unidade(Long id, String numero, Bloco bloco) {
        Unidade u = new Unidade();
        u.setId(id);
        u.setNumero(numero);
        u.setBloco(bloco);
        return u;
    }

    private static AreaComum area(Long id, String nome, Condominio condominio, boolean exigeAprovacao) {
        AreaComum a = new AreaComum();
        a.setId(id);
        a.setNome(nome);
        a.setCondominio(condominio);
        a.setCapacidade(50);
        a.setHorarioAbertura(LocalTime.of(8, 0));
        a.setHorarioFechamento(LocalTime.of(22, 0));
        a.setExigeAprovacao(exigeAprovacao);
        return a;
    }

    private static Reserva reserva(Long id, Unidade unidade, AreaComum area, LocalDate data, LocalTime inicio,
                                   LocalTime fim, SituacaoReserva situacao) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setUnidade(unidade);
        r.setAreaComum(area);
        r.setData(data);
        r.setHoraInicio(inicio);
        r.setHoraFim(fim);
        r.setFinalidade("Evento");
        r.setSituacao(situacao);
        return r;
    }

    private static Cobranca cobranca(SituacaoCobranca situacao, LocalDate vencimento) {
        Cobranca c = new Cobranca();
        c.setSituacao(situacao);
        c.setVencimento(vencimento);
        return c;
    }
}
