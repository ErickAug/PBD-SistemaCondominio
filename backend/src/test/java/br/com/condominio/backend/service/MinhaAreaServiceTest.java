package br.com.condominio.backend.service;

import br.com.condominio.backend.dto.RelatorioPeriodoDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.RateioMensal;
import br.com.condominio.backend.model.RateioUnidade;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.AlcanceAviso;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import br.com.condominio.backend.repository.AvisoRepository;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinhaAreaServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private VinculoMoradorRepository vinculoMoradorRepository;
    @Mock
    private MoradiaService moradiaService;
    @Mock
    private AvisoService avisoService;
    @Mock
    private AvisoRepository avisoRepository;
    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private ReservaService reservaService;
    @Mock
    private OcorrenciaMoradorService ocorrenciaMoradorService;
    @Mock
    private RelatorioService relatorioService;

    @InjectMocks
    private MinhaAreaService service;

    private Condominio condominio;
    private Bloco blocoA;
    private Bloco blocoB;
    private Usuario inquilino;
    private Usuario donoQueNaoMora;
    private Usuario donoQueMora;
    private Unidade alugada;
    private Unidade ondeODonoMora;
    private Unidade outraDoDono;
    private RelatorioPeriodoDTO relatorio;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);
        blocoA = bloco(10L, "A", condominio);
        blocoB = bloco(11L, "B", condominio);

        inquilino = usuario(1L, "Inquilino", Perfil.MORADOR);
        donoQueNaoMora = usuario(2L, "Dono distante", Perfil.PROPRIETARIO);
        donoQueMora = usuario(3L, "Dono presente", Perfil.PROPRIETARIO);

        alugada = unidade(101L, "101", blocoA, donoQueNaoMora);
        ondeODonoMora = unidade(102L, "102", blocoA, donoQueMora);
        outraDoDono = unidade(201L, "201", blocoB, donoQueMora);

        relatorio = new RelatorioPeriodoDTO(null, null, BigDecimal.ZERO);

        lenient().when(moradiaService.moraNaUnidade(any(Usuario.class), anyLong())).thenReturn(false);
        lenient().when(moradiaService.moraNaUnidade(inquilino, 101L)).thenReturn(true);
        lenient().when(moradiaService.moraNaUnidade(donoQueMora, 102L)).thenReturn(true);
        lenient().when(unidadeRepository.findById(101L)).thenReturn(Optional.of(alugada));
        lenient().when(unidadeRepository.findById(102L)).thenReturn(Optional.of(ondeODonoMora));
        lenient().when(unidadeRepository.findById(201L)).thenReturn(Optional.of(outraDoDono));
        lenient().when(avisoService.listarAvisosDoUsuario(eq(CONDOMINIO_ID), any())).thenReturn(List.of());
        lenient().when(avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(CONDOMINIO_ID)).thenReturn(List.of());
        lenient().when(cobrancaRepository.findByUnidadeId(anyLong())).thenReturn(List.of());
        lenient().when(ocorrenciaMoradorService.listarDaUnidade(any(), any())).thenReturn(List.of());
        lenient().when(relatorioService.gerarRelatorio(eq(CONDOMINIO_ID), any())).thenReturn(relatorio);
    }

    @Test
    void proprietarioDeMaisDeUmaUnidadeVeTodasParaEscolher() {
        when(unidadeRepository.findByProprietarioId(3L)).thenReturn(List.of(outraDoDono, ondeODonoMora));

        List<MinhaAreaService.MinhaUnidade> unidades = service.listarMinhasUnidades(CONDOMINIO_ID, donoQueMora);

        assertEquals(List.of(102L, 201L), unidades.stream().map(m -> m.unidade().getId()).toList());
        assertTrue(unidades.get(0).mora() && unidades.get(0).dono());
        assertTrue(!unidades.get(1).mora() && unidades.get(1).dono());
    }

    @Test
    void moradorVeAUnidadeOndeTemVinculoAtivo() {
        VinculoMorador vinculo = new VinculoMorador();
        vinculo.setUnidade(alugada);
        vinculo.setMorador(inquilino);
        when(vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(1L)).thenReturn(List.of(vinculo));

        List<MinhaAreaService.MinhaUnidade> unidades = service.listarMinhasUnidades(CONDOMINIO_ID, inquilino);

        assertEquals(1, unidades.size());
        assertSame(alugada, unidades.get(0).unidade());
        assertTrue(unidades.get(0).mora());
        assertFalse(unidades.get(0).dono());
    }

    @Test
    void inquilinoQueSaiuNaoTemMaisUnidade() {
        when(vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(1L)).thenReturn(List.of());

        assertTrue(service.listarMinhasUnidades(CONDOMINIO_ID, inquilino).isEmpty());
    }

    @Test
    void unidadeDeOutroCondominioNaoEntraNaLista() {
        Condominio outro = new Condominio();
        outro.setId(2L);
        Unidade deFora = unidade(900L, "900", bloco(90L, "X", outro), donoQueMora);
        when(unidadeRepository.findByProprietarioId(3L)).thenReturn(List.of(deFora));

        assertTrue(service.listarMinhasUnidades(CONDOMINIO_ID, donoQueMora).isEmpty());
    }

    @Test
    void painelDoMoradorTemAvisosTaxaReservasEOcorrencias() {
        Reserva reserva = new Reserva();
        when(reservaService.listarDaUnidade(101L)).thenReturn(List.of(reserva));
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(1L, inquilino, donoQueNaoMora, "300.00", "100.00", SituacaoCobranca.ABERTA, LocalDate.now().plusDays(5))));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, inquilino, null);

        assertEquals(List.of("MORADOR"), painel.papeis());
        assertTrue(painel.permissoes().autorizarVisita());
        assertTrue(painel.permissoes().reservarAreaComum());
        assertTrue(painel.permissoes().abrirOcorrencia());
        assertEquals(List.of(reserva), painel.reservas());
        verify(ocorrenciaMoradorService).listarDaUnidade(alugada, inquilino);
        verify(avisoService).listarAvisosDoUsuario(CONDOMINIO_ID, inquilino);

        MinhaAreaService.CobrancaDoPainel taxa = painel.cobrancas().get(0);
        assertEquals(new BigDecimal("300.00"), taxa.parteOrdinaria());
        assertNull(taxa.parteExtraordinaria());
        assertNull(taxa.valorTotal());
    }

    @Test
    void moradorSoVeAsCobrancasDoPeriodoEmQueEraOcupante() {
        Usuario antigo = usuario(50L, "Inquilino anterior", Perfil.MORADOR);
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(1L, antigo, donoQueNaoMora, "300.00", "0.00", SituacaoCobranca.PAGA, LocalDate.now().minusMonths(3)),
                cobranca(2L, inquilino, donoQueNaoMora, "310.00", "0.00", SituacaoCobranca.ABERTA, LocalDate.now().plusDays(5))));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, inquilino, null);

        assertEquals(List.of(2L), painel.cobrancas().stream().map(c -> c.cobranca().getId()).toList());
    }

    @Test
    void painelDoProprietarioQueNaoMoraTemSituacaoFinanceiraEPrestacaoDeContas() {
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(1L, inquilino, donoQueNaoMora, "300.00", "100.00", SituacaoCobranca.ABERTA, LocalDate.now().minusDays(10)),
                cobranca(2L, inquilino, donoQueNaoMora, "300.00", "100.00", SituacaoCobranca.PAGA, LocalDate.now().minusDays(40))));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, donoQueNaoMora, null);

        assertEquals(List.of("PROPRIETARIO"), painel.papeis());
        MinhaAreaService.CobrancaDoPainel atrasada = painel.cobrancas().get(0);
        assertNull(atrasada.parteOrdinaria());
        assertEquals(new BigDecimal("100.00"), atrasada.parteExtraordinaria());
        assertEquals(new BigDecimal("400.00"), atrasada.valorTotal());
        assertTrue(atrasada.emAtraso());
        assertEquals(10L, atrasada.diasAtraso());

        assertFalse(painel.situacaoFinanceira().emDia());
        assertEquals(1, painel.situacaoFinanceira().cobrancasEmAtraso());
        assertEquals(new BigDecimal("400.00"), painel.situacaoFinanceira().valorEmAtraso());
        assertSame(relatorio, painel.prestacaoContas());
    }

    @Test
    void proprietarioQueNaoMoraNaoAutorizaVisitaNemReserva() {
        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, donoQueNaoMora, null);

        assertFalse(painel.permissoes().autorizarVisita());
        assertFalse(painel.permissoes().reservarAreaComum());
        assertTrue(painel.permissoes().abrirOcorrencia());
        assertTrue(painel.permissoes().verPrestacaoContas());
        assertNull(painel.reservas());
        verify(reservaService, never()).listarDaUnidade(anyLong());
    }

    @Test
    void proprietarioQueNaoMoraVeSoOsAvisosGeraisESemConfirmar() {
        Aviso geral = aviso(1L, AlcanceAviso.CONDOMINIO, null, null, LocalDate.now().plusDays(5));
        Aviso doBloco = aviso(2L, AlcanceAviso.BLOCO, blocoA, null, LocalDate.now().plusDays(5));
        Aviso vencido = aviso(3L, AlcanceAviso.CONDOMINIO, null, null, LocalDate.now().minusDays(1));
        when(avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(CONDOMINIO_ID)).thenReturn(List.of(geral, doBloco, vencido));
        when(avisoService.estaVencido(geral)).thenReturn(false);
        when(avisoService.estaVencido(vencido)).thenReturn(true);

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, donoQueNaoMora, null);

        assertEquals(List.of(1L), painel.avisos().stream().map(a -> a.aviso().getId()).toList());
        assertFalse(painel.avisos().get(0).confirmavel());
    }

    @Test
    void quemEhDonoEMoraVeOsDoisConjuntosSemRepetir() {
        Aviso geral = aviso(1L, AlcanceAviso.CONDOMINIO, null, null, LocalDate.now().plusDays(5));
        LocalDateTime leitura = LocalDateTime.now().minusHours(1);
        when(avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, donoQueMora))
                .thenReturn(List.of(new AvisoService.AvisoDoUsuario(geral, leitura)));
        when(avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(CONDOMINIO_ID)).thenReturn(List.of(geral));
        when(cobrancaRepository.findByUnidadeId(102L)).thenReturn(List.of(
                cobranca(1L, donoQueMora, donoQueMora, "300.00", "100.00", SituacaoCobranca.ABERTA, LocalDate.now().plusDays(5))));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 102L, donoQueMora, null);

        assertEquals(List.of("MORADOR", "PROPRIETARIO"), painel.papeis());
        assertEquals(1, painel.avisos().size());
        assertTrue(painel.avisos().get(0).confirmavel());
        assertEquals(leitura, painel.avisos().get(0).dataLeitura());
        assertEquals(1, painel.cobrancas().size());
        MinhaAreaService.CobrancaDoPainel taxa = painel.cobrancas().get(0);
        assertEquals(new BigDecimal("300.00"), taxa.parteOrdinaria());
        assertEquals(new BigDecimal("100.00"), taxa.parteExtraordinaria());
        assertEquals(new BigDecimal("400.00"), taxa.valorTotal());
        assertTrue(painel.permissoes().reservarAreaComum());
    }

    @Test
    void avisosDeOutraUnidadeOuOutroBlocoNaoEntramNoPainel() {
        Aviso daOutraUnidade = aviso(5L, AlcanceAviso.UNIDADE, blocoB, outraDoDono, LocalDate.now().plusDays(5));
        Aviso doOutroBloco = aviso(6L, AlcanceAviso.BLOCO, blocoB, null, LocalDate.now().plusDays(5));
        Aviso destaUnidade = aviso(7L, AlcanceAviso.UNIDADE, blocoA, ondeODonoMora, LocalDate.now().plusDays(5));
        when(avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, donoQueMora)).thenReturn(List.of(
                new AvisoService.AvisoDoUsuario(daOutraUnidade, null),
                new AvisoService.AvisoDoUsuario(doOutroBloco, null),
                new AvisoService.AvisoDoUsuario(destaUnidade, null)));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 102L, donoQueMora, null);

        assertEquals(List.of(7L), painel.avisos().stream().map(a -> a.aviso().getId()).toList());
    }

    @Test
    void trocarOIdentificadorParaUnidadeAlheiaEhNegado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.montarPainel(CONDOMINIO_ID, 102L, inquilino, null));

        assertEquals("Acesso negado a esta unidade.", erro.getMessage());
    }

    @Test
    void unidadeDeOutroCondominioOuInexistenteEhNegada() {
        Condominio outro = new Condominio();
        outro.setId(2L);
        when(unidadeRepository.findById(900L)).thenReturn(Optional.of(unidade(900L, "900", bloco(90L, "X", outro), donoQueMora)));
        when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals("Acesso negado a esta unidade.", assertThrows(RegraDeNegocioException.class,
                () -> service.montarPainel(CONDOMINIO_ID, 900L, donoQueMora, null)).getMessage());
        assertEquals("Acesso negado a esta unidade.", assertThrows(RegraDeNegocioException.class,
                () -> service.montarPainel(CONDOMINIO_ID, 999L, donoQueMora, null)).getMessage());
    }

    @Test
    void inquilinoQueSaiuPerdeOAcessoAUnidade() {
        when(moradiaService.moraNaUnidade(inquilino, 101L)).thenReturn(false);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.montarPainel(CONDOMINIO_ID, 101L, inquilino, null));

        assertEquals("Acesso negado a esta unidade.", erro.getMessage());
    }

    @Test
    void prestacaoDeContasUsaOMesAnteriorPorPadrao() {
        service.montarPainel(CONDOMINIO_ID, 101L, inquilino, null);

        verify(relatorioService).gerarRelatorio(CONDOMINIO_ID, LocalDate.now().minusMonths(1).withDayOfMonth(1));
    }

    @Test
    void prestacaoDeContasUsaACompetenciaInformada() {
        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, inquilino, LocalDate.of(2026, 7, 19));

        assertEquals(LocalDate.of(2026, 7, 1), painel.competencia());
        verify(relatorioService).gerarRelatorio(CONDOMINIO_ID, LocalDate.of(2026, 7, 1));
    }

    @Test
    void semCobrancaEmAtrasoAUnidadeEstaEmDia() {
        when(cobrancaRepository.findByUnidadeId(101L)).thenReturn(List.of(
                cobranca(1L, inquilino, donoQueNaoMora, "300.00", "0.00", SituacaoCobranca.PAGA, LocalDate.now().minusDays(20))));

        MinhaAreaService.Painel painel = service.montarPainel(CONDOMINIO_ID, 101L, inquilino, null);

        assertTrue(painel.situacaoFinanceira().emDia());
        assertEquals(BigDecimal.ZERO, painel.situacaoFinanceira().valorEmAtraso());
    }

    private static Bloco bloco(Long id, String nome, Condominio condominio) {
        Bloco b = new Bloco();
        b.setId(id);
        b.setNome(nome);
        b.setCondominio(condominio);
        return b;
    }

    private static Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setPerfil(perfil);
        return u;
    }

    private static Unidade unidade(Long id, String numero, Bloco bloco, Usuario dono) {
        Unidade u = new Unidade();
        u.setId(id);
        u.setNumero(numero);
        u.setBloco(bloco);
        u.setProprietario(dono);
        return u;
    }

    private Aviso aviso(Long id, AlcanceAviso alcance, Bloco bloco, Unidade unidade, LocalDate validade) {
        Aviso a = new Aviso();
        a.setId(id);
        a.setCondominio(condominio);
        a.setAlcance(alcance);
        a.setBloco(bloco);
        a.setUnidade(unidade);
        a.setDataValidade(validade);
        a.setDataCriacao(LocalDateTime.now().minusDays(id));
        return a;
    }

    private static Cobranca cobranca(Long id, Usuario respOrdinario, Usuario respExtra, String ordinario,
                                     String extra, SituacaoCobranca situacao, LocalDate vencimento) {
        RateioMensal mensal = new RateioMensal();
        mensal.setCompetencia(vencimento.minusMonths(1).withDayOfMonth(1));

        RateioUnidade ru = new RateioUnidade();
        ru.setRateioMensal(mensal);
        ru.setValorOrdinario(new BigDecimal(ordinario));
        ru.setValorExtraordinario(new BigDecimal(extra));
        ru.setResponsavelOrdinario(respOrdinario);
        ru.setResponsavelExtraordinario(respExtra);

        Cobranca c = new Cobranca();
        c.setId(id);
        c.setRateioUnidade(ru);
        c.setSituacao(situacao);
        c.setVencimento(vencimento);
        return c;
    }
}
