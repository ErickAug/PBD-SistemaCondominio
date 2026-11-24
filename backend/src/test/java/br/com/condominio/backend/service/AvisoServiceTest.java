package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.AvisoLeitura;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.AlcanceAviso;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.repository.AvisoLeituraRepository;
import br.com.condominio.backend.repository.AvisoRepository;
import br.com.condominio.backend.repository.BlocoRepository;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoServiceTest {

    private static final Long CONDOMINIO_ID = 1L;
    private static final Long OUTRO_CONDOMINIO_ID = 2L;

    @Mock
    private AvisoRepository avisoRepository;
    @Mock
    private AvisoLeituraRepository avisoLeituraRepository;
    @Mock
    private BlocoRepository blocoRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private OcupacaoRepository ocupacaoRepository;
    @Mock
    private VinculoMoradorRepository vinculoMoradorRepository;

    @InjectMocks
    private AvisoService avisoService;

    private Condominio condominio;
    private Condominio outroCondominio;
    private Bloco blocoA;
    private Bloco blocoDeOutroCondominio;
    private Usuario sindico;

    private Usuario donoDaAlugada;
    private Usuario inquilino;
    private Usuario donoMorando;
    private Usuario familiarDoDono;
    private Usuario donoDaVazia;

    private Unidade alugada;
    private Unidade proprietarioMorando;
    private Unidade vazia;
    private Unidade semOcupacaoESemDono;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);

        outroCondominio = new Condominio();
        outroCondominio.setId(OUTRO_CONDOMINIO_ID);

        blocoA = bloco(10L, "A", condominio);
        blocoDeOutroCondominio = bloco(20L, "X", outroCondominio);

        sindico = usuario(1L, "Sindico", Perfil.SINDICO);
        donoDaAlugada = usuario(2L, "Dono da 101", Perfil.PROPRIETARIO);
        inquilino = usuario(3L, "Inquilino da 101", Perfil.MORADOR);
        donoMorando = usuario(4L, "Dono da 102", Perfil.PROPRIETARIO);
        familiarDoDono = usuario(5L, "Familiar da 102", Perfil.MORADOR);
        donoDaVazia = usuario(6L, "Dono da 103", Perfil.PROPRIETARIO);

        alugada = unidade(101L, "101", blocoA, donoDaAlugada);
        proprietarioMorando = unidade(102L, "102", blocoA, donoMorando);
        vazia = unidade(103L, "103", blocoA, donoDaVazia);
        semOcupacaoESemDono = unidade(104L, "104", blocoA, null);

        List<Ocupacao> ocupacoes = List.of(
                ocupacao(alugada, TipoOcupacao.ALUGADA, inquilino),
                ocupacao(proprietarioMorando, TipoOcupacao.PROPRIETARIO_MORANDO, donoMorando),
                ocupacao(vazia, TipoOcupacao.VAZIA, null));

        List<VinculoMorador> vinculos = List.of(
                vinculo(alugada, inquilino),
                vinculo(proprietarioMorando, familiarDoDono));

        lenient().when(ocupacaoRepository.buscarOcupacoesAtuais(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return ocupacoes.stream().filter(o -> ids.contains(o.getUnidade().getId())).toList();
        });
        lenient().when(vinculoMoradorRepository.findByUnidadeIdInAndDataSaidaIsNull(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return vinculos.stream().filter(v -> ids.contains(v.getUnidade().getId())).toList();
        });
        lenient().when(vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(any())).thenAnswer(inv -> {
            Long moradorId = inv.getArgument(0);
            return vinculos.stream().filter(v -> v.getMorador().getId().equals(moradorId)).toList();
        });
        lenient().when(unidadeRepository.findByProprietarioId(any())).thenAnswer(inv -> {
            Long donoId = inv.getArgument(0);
            return todasAsUnidades().stream()
                    .filter(u -> u.getProprietario() != null && u.getProprietario().getId().equals(donoId))
                    .toList();
        });
        lenient().when(unidadeRepository.findByBlocoCondominioId(CONDOMINIO_ID)).thenReturn(todasAsUnidades());
        lenient().when(unidadeRepository.findByBlocoId(blocoA.getId())).thenReturn(todasAsUnidades());
        lenient().when(avisoRepository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(avisoLeituraRepository.save(any(AvisoLeitura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criarAvisoParaOCondominioGuardaTituloTextoAlcanceValidadeEAutor() {
        LocalDate validade = LocalDate.now().plusDays(5);

        Aviso aviso = avisoService.criar(condominio, sindico, "Corte de agua", "A agua sera cortada as 8h.",
                AlcanceAviso.CONDOMINIO, null, null, validade);

        assertEquals("Corte de agua", aviso.getTitulo());
        assertEquals("A agua sera cortada as 8h.", aviso.getTexto());
        assertEquals(AlcanceAviso.CONDOMINIO, aviso.getAlcance());
        assertEquals(validade, aviso.getDataValidade());
        assertSame(sindico, aviso.getAutor());
        assertSame(condominio, aviso.getCondominio());
        assertNotNull(aviso.getDataCriacao());
        assertNull(aviso.getBloco());
        assertNull(aviso.getUnidade());
    }

    @Test
    void criarAvisoParaUmBlocoDoCondominioVinculaOBloco() {
        when(blocoRepository.findById(blocoA.getId())).thenReturn(Optional.of(blocoA));

        Aviso aviso = avisoService.criar(condominio, sindico, "Pintura", "Pintura do hall do bloco A.",
                AlcanceAviso.BLOCO, blocoA.getId(), null, LocalDate.now());

        assertSame(blocoA, aviso.getBloco());
        assertNull(aviso.getUnidade());
    }

    @Test
    void criarAvisoParaUmaUnidadeVinculaUnidadeEBloco() {
        when(unidadeRepository.findById(alugada.getId())).thenReturn(Optional.of(alugada));

        Aviso aviso = avisoService.criar(condominio, sindico, "Vazamento", "Vazamento vindo da 101.",
                AlcanceAviso.UNIDADE, null, alugada.getId(), LocalDate.now().plusDays(1));

        assertSame(alugada, aviso.getUnidade());
        assertSame(blocoA, aviso.getBloco());
    }

    @Test
    void criarAvisoDeBlocoSemInformarOBlocoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "T", "Texto", AlcanceAviso.BLOCO, null, null,
                        LocalDate.now()));

        assertEquals("Informe o bloco do aviso.", erro.getMessage());
    }

    @Test
    void criarAvisoDeUnidadeSemInformarAUnidadeEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "T", "Texto", AlcanceAviso.UNIDADE, null, null,
                        LocalDate.now()));

        assertEquals("Informe a unidade do aviso.", erro.getMessage());
    }

    @Test
    void criarAvisoComBlocoDeOutroCondominioEhRecusado() {
        when(blocoRepository.findById(blocoDeOutroCondominio.getId())).thenReturn(Optional.of(blocoDeOutroCondominio));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "T", "Texto", AlcanceAviso.BLOCO,
                        blocoDeOutroCondominio.getId(), null, LocalDate.now()));

        assertEquals("Este bloco não pertence a este condomínio.", erro.getMessage());
        verify(avisoRepository, never()).save(any());
    }

    @Test
    void criarAvisoComUnidadeDeOutroCondominioEhRecusado() {
        Unidade deFora = unidade(900L, "900", blocoDeOutroCondominio, null);
        when(unidadeRepository.findById(900L)).thenReturn(Optional.of(deFora));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "T", "Texto", AlcanceAviso.UNIDADE, null, 900L,
                        LocalDate.now()));

        assertEquals("Esta unidade não pertence a este condomínio.", erro.getMessage());
        verify(avisoRepository, never()).save(any());
    }

    @Test
    void criarAvisoSemTituloEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, " ", "Texto", AlcanceAviso.CONDOMINIO, null, null,
                        LocalDate.now()));

        assertEquals("Informe o título do aviso.", erro.getMessage());
    }

    @Test
    void criarAvisoSemTextoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "Titulo", null, AlcanceAviso.CONDOMINIO, null, null,
                        LocalDate.now()));

        assertEquals("Informe o texto do aviso.", erro.getMessage());
    }

    @Test
    void criarAvisoSemAlcanceEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "Titulo", "Texto", null, null, null, LocalDate.now()));

        assertEquals("Informe o alcance do aviso: CONDOMINIO, BLOCO ou UNIDADE.", erro.getMessage());
    }

    @Test
    void criarAvisoSemValidadeEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "Titulo", "Texto", AlcanceAviso.CONDOMINIO, null, null,
                        null));

        assertEquals("Informe a data de validade do aviso.", erro.getMessage());
    }

    @Test
    void criarAvisoComValidadeNoPassadoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.criar(condominio, sindico, "Titulo", "Texto", AlcanceAviso.CONDOMINIO, null, null,
                        LocalDate.now().minusDays(1)));

        assertEquals("A validade do aviso não pode ser anterior a hoje.", erro.getMessage());
    }

    @Test
    void unidadeAlugadaRecebeNoInquilinoENaoNoProprietarioQueNaoMora() {
        Aviso aviso = avisoDeUnidade(alugada);

        List<AvisoService.Destinatario> destinatarios = avisoService.listarDestinatarios(aviso);

        assertEquals(List.of(inquilino.getId()), idsDosUsuarios(destinatarios));
        assertEquals(AvisoService.PAPEL_MORADOR, destinatarios.get(0).papel());
    }

    @Test
    void unidadeComProprietarioMorandoRecebeNoDonoENosMoradoresDaCasa() {
        Aviso aviso = avisoDeUnidade(proprietarioMorando);

        List<AvisoService.Destinatario> destinatarios = avisoService.listarDestinatarios(aviso);

        assertEquals(List.of(donoMorando.getId(), familiarDoDono.getId()), idsDosUsuarios(destinatarios));
        assertTrue(destinatarios.stream().allMatch(d -> AvisoService.PAPEL_MORADOR.equals(d.papel())));
    }

    @Test
    void unidadeVaziaNaoRecebeAvisoDeMoradorEOProprietarioRecebe() {
        Aviso aviso = avisoDeUnidade(vazia);

        List<AvisoService.Destinatario> destinatarios = avisoService.listarDestinatarios(aviso);

        assertEquals(List.of(donoDaVazia.getId()), idsDosUsuarios(destinatarios));
        assertEquals(AvisoService.PAPEL_PROPRIETARIO, destinatarios.get(0).papel());
    }

    @Test
    void unidadeSemOcupacaoESemProprietarioNaoTemDestinatario() {
        Aviso aviso = avisoDeUnidade(semOcupacaoESemDono);

        assertTrue(avisoService.listarDestinatarios(aviso).isEmpty());
    }

    @Test
    void avisoDoCondominioChegaAQuemMoraEAoDonoDaVaziaSemIncluirODonoDaAlugada() {
        Aviso aviso = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().plusDays(3));

        List<Long> ids = idsDosUsuarios(avisoService.listarDestinatarios(aviso));

        assertEquals(List.of(inquilino.getId(), donoMorando.getId(), familiarDoDono.getId(), donoDaVazia.getId()), ids);
        assertFalse(ids.contains(donoDaAlugada.getId()));
    }

    @Test
    void avisoDeBlocoUsaAsUnidadesDoBloco() {
        Aviso aviso = avisoComAlcance(AlcanceAviso.BLOCO, LocalDate.now());
        aviso.setBloco(blocoA);

        List<Long> ids = idsDosUsuarios(avisoService.listarDestinatarios(aviso));

        assertEquals(4, ids.size());
        verify(unidadeRepository).findByBlocoId(blocoA.getId());
    }

    @Test
    void moradorConfirmaLeituraEAConfirmacaoFicaRegistradaComData() {
        Aviso aviso = avisoDeUnidade(alugada);
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));
        when(avisoLeituraRepository.findByAvisoIdAndUsuarioId(aviso.getId(), inquilino.getId()))
                .thenReturn(Optional.empty());

        AvisoLeitura leitura = avisoService.confirmarLeitura(CONDOMINIO_ID, aviso.getId(), inquilino);

        ArgumentCaptor<AvisoLeitura> captor = ArgumentCaptor.forClass(AvisoLeitura.class);
        verify(avisoLeituraRepository).save(captor.capture());
        assertSame(aviso, captor.getValue().getAviso());
        assertSame(inquilino, captor.getValue().getUsuario());
        assertSame(alugada, captor.getValue().getUnidade());
        assertNotNull(leitura.getDataLeitura());
    }

    @Test
    void confirmarLeituraDuasVezesDevolveAConfirmacaoOriginalSemDuplicar() {
        Aviso aviso = avisoDeUnidade(alugada);
        AvisoLeitura anterior = leitura(aviso, inquilino, alugada, LocalDateTime.now().minusHours(2));
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));
        when(avisoLeituraRepository.findByAvisoIdAndUsuarioId(aviso.getId(), inquilino.getId()))
                .thenReturn(Optional.of(anterior));

        AvisoLeitura leitura = avisoService.confirmarLeitura(CONDOMINIO_ID, aviso.getId(), inquilino);

        assertSame(anterior, leitura);
        verify(avisoLeituraRepository, never()).save(any());
    }

    @Test
    void proprietarioQueNaoMoraNaoConsegueConfirmarLeituraDeAvisoDaUnidadeAlugada() {
        Aviso aviso = avisoDeUnidade(alugada);
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.confirmarLeitura(CONDOMINIO_ID, aviso.getId(), donoDaAlugada));

        assertEquals("Este aviso não é destinado a você.", erro.getMessage());
        verify(avisoLeituraRepository, never()).save(any());
    }

    @Test
    void confirmarLeituraDeAvisoVencidoEhRecusado() {
        Aviso aviso = avisoDeUnidade(alugada);
        aviso.setDataValidade(LocalDate.now().minusDays(1));
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.confirmarLeitura(CONDOMINIO_ID, aviso.getId(), inquilino));

        assertEquals("Este aviso está vencido e não aceita mais confirmação de leitura.", erro.getMessage());
    }

    @Test
    void avisoDeOutroCondominioNaoEhEncontrado() {
        when(avisoRepository.findByIdAndCondominioId(50L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> avisoService.confirmarLeitura(CONDOMINIO_ID, 50L, inquilino));

        assertEquals("Aviso não encontrado.", erro.getMessage());
    }

    @Test
    void relatorioSeparaQuemLeuDeQuemNaoLeu() {
        Aviso aviso = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().plusDays(3));
        LocalDateTime quandoLeu = LocalDateTime.now().minusMinutes(30);
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));
        when(avisoLeituraRepository.findByAvisoId(aviso.getId()))
                .thenReturn(List.of(leitura(aviso, inquilino, alugada, quandoLeu)));

        AvisoService.RelatorioLeitura relatorio = avisoService.gerarRelatorioLeitura(CONDOMINIO_ID, aviso.getId());

        assertEquals(1, relatorio.leram().size());
        assertEquals(inquilino.getId(), relatorio.leram().get(0).usuario().getId());
        assertEquals(quandoLeu, relatorio.leram().get(0).dataLeitura());
        assertEquals(List.of(donoMorando.getId(), familiarDoDono.getId(), donoDaVazia.getId()),
                relatorio.naoLeram().stream().map(s -> s.usuario().getId()).toList());
        assertTrue(relatorio.naoLeram().stream().allMatch(s -> s.dataLeitura() == null));
    }

    @Test
    void relatorioMantemQuemLeuMesmoSeNaoForMaisDestinatario() {
        Aviso aviso = avisoDeUnidade(vazia);
        Usuario exInquilino = usuario(99L, "Ex inquilino", Perfil.MORADOR);
        when(avisoRepository.findByIdAndCondominioId(aviso.getId(), CONDOMINIO_ID)).thenReturn(Optional.of(aviso));
        when(avisoLeituraRepository.findByAvisoId(aviso.getId()))
                .thenReturn(List.of(leitura(aviso, exInquilino, vazia, LocalDateTime.now().minusDays(1))));

        AvisoService.RelatorioLeitura relatorio = avisoService.gerarRelatorioLeitura(CONDOMINIO_ID, aviso.getId());

        assertEquals(AvisoService.PAPEL_ANTIGO_DESTINATARIO, relatorio.leram().get(0).papel());
        assertEquals(List.of(donoDaVazia.getId()),
                relatorio.naoLeram().stream().map(s -> s.usuario().getId()).toList());
    }

    @Test
    void relatorioDeAvisoDeOutroCondominioEhRecusado() {
        when(avisoRepository.findByIdAndCondominioId(7L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class, () -> avisoService.gerarRelatorioLeitura(CONDOMINIO_ID, 7L));
    }

    @Test
    void inquilinoVeOsAvisosVigentesDaSuaUnidadeComASituacaoDeLeitura() {
        Aviso lido = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().plusDays(2));
        lido.setId(500L);
        Aviso naoLido = avisoDeUnidade(alugada);
        naoLido.setId(501L);
        LocalDateTime quandoLeu = LocalDateTime.now().minusHours(1);

        when(avisoRepository.listarVigentesParaUnidades(eq(CONDOMINIO_ID), eq(LocalDate.now()),
                eq(Set.of(blocoA.getId())), eq(Set.of(alugada.getId()))))
                .thenReturn(List.of(lido, naoLido));
        when(avisoLeituraRepository.findByUsuarioIdAndAvisoIdIn(eq(inquilino.getId()), anyCollection()))
                .thenReturn(List.of(leitura(lido, inquilino, alugada, quandoLeu)));

        List<AvisoService.AvisoDoUsuario> avisos = avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, inquilino);

        assertEquals(2, avisos.size());
        assertEquals(quandoLeu, avisos.get(0).dataLeitura());
        assertNull(avisos.get(1).dataLeitura());
    }

    @Test
    void proprietarioQueNaoMoraNaoRecebeAvisosDaUnidadeAlugada() {
        List<AvisoService.AvisoDoUsuario> avisos = avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, donoDaAlugada);

        assertTrue(avisos.isEmpty());
        verify(avisoRepository, never()).listarVigentesParaUnidades(any(), any(), anyCollection(), anyCollection());
    }

    @Test
    void proprietarioDeUnidadeVaziaRecebeOsAvisosDela() {
        Aviso aviso = avisoDeUnidade(vazia);
        when(avisoRepository.listarVigentesParaUnidades(eq(CONDOMINIO_ID), eq(LocalDate.now()),
                eq(Set.of(blocoA.getId())), eq(Set.of(vazia.getId()))))
                .thenReturn(List.of(aviso));
        when(avisoLeituraRepository.findByUsuarioIdAndAvisoIdIn(eq(donoDaVazia.getId()), anyCollection()))
                .thenReturn(List.of());

        List<AvisoService.AvisoDoUsuario> avisos = avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, donoDaVazia);

        assertEquals(1, avisos.size());
        assertSame(aviso, avisos.get(0).aviso());
    }

    @Test
    void usuarioNaoRecebeAvisosDeUnidadeDeOutroCondominio() {
        Usuario moradorDeFora = usuario(70L, "Morador de fora", Perfil.MORADOR);
        Unidade unidadeDeFora = unidade(901L, "901", blocoDeOutroCondominio, null);
        when(vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(moradorDeFora.getId()))
                .thenReturn(List.of(vinculo(unidadeDeFora, moradorDeFora)));

        List<AvisoService.AvisoDoUsuario> avisos = avisoService.listarAvisosDoUsuario(CONDOMINIO_ID, moradorDeFora);

        assertTrue(avisos.isEmpty());
    }

    @Test
    void avisoVencidoAteOntemEstaVencidoEOQueValeHojeNao() {
        Aviso venceuOntem = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().minusDays(1));
        Aviso valeHoje = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now());

        assertTrue(avisoService.estaVencido(venceuOntem));
        assertFalse(avisoService.estaVencido(valeHoje));
    }

    @Test
    void historicoDoSindicoTrazTodosOsAvisosDoCondominioInclusiveVencidos() {
        Aviso vigente = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().plusDays(1));
        Aviso vencido = avisoComAlcance(AlcanceAviso.CONDOMINIO, LocalDate.now().minusDays(10));
        when(avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(CONDOMINIO_ID))
                .thenReturn(List.of(vigente, vencido));

        List<Aviso> historico = avisoService.listarHistorico(CONDOMINIO_ID);

        assertEquals(List.of(vigente, vencido), historico);
    }

    private List<Unidade> todasAsUnidades() {
        return List.of(alugada, proprietarioMorando, vazia, semOcupacaoESemDono);
    }

    private List<Long> idsDosUsuarios(List<AvisoService.Destinatario> destinatarios) {
        return destinatarios.stream().map(d -> d.usuario().getId()).toList();
    }

    private Aviso avisoComAlcance(AlcanceAviso alcance, LocalDate validade) {
        Aviso aviso = new Aviso();
        aviso.setId(300L);
        aviso.setCondominio(condominio);
        aviso.setAutor(sindico);
        aviso.setTitulo("Aviso");
        aviso.setTexto("Texto do aviso");
        aviso.setAlcance(alcance);
        aviso.setDataValidade(validade);
        aviso.setDataCriacao(LocalDateTime.now());
        return aviso;
    }

    private Aviso avisoDeUnidade(Unidade unidade) {
        Aviso aviso = avisoComAlcance(AlcanceAviso.UNIDADE, LocalDate.now().plusDays(1));
        aviso.setUnidade(unidade);
        aviso.setBloco(unidade.getBloco());
        return aviso;
    }

    private static Bloco bloco(Long id, String nome, Condominio condominio) {
        Bloco bloco = new Bloco();
        bloco.setId(id);
        bloco.setNome(nome);
        bloco.setCondominio(condominio);
        return bloco;
    }

    private static Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setUsuario("usuario" + id);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Unidade unidade(Long id, String numero, Bloco bloco, Usuario proprietario) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setNumero(numero);
        unidade.setBloco(bloco);
        unidade.setProprietario(proprietario);
        return unidade;
    }

    private static Ocupacao ocupacao(Unidade unidade, TipoOcupacao tipo, Usuario ocupante) {
        Ocupacao ocupacao = new Ocupacao();
        ocupacao.setUnidade(unidade);
        ocupacao.setTipo(tipo);
        ocupacao.setOcupante(ocupante);
        ocupacao.setDataEntrada(LocalDate.now().minusMonths(1));
        return ocupacao;
    }

    private static VinculoMorador vinculo(Unidade unidade, Usuario morador) {
        VinculoMorador vinculo = new VinculoMorador();
        vinculo.setUnidade(unidade);
        vinculo.setMorador(morador);
        vinculo.setDataEntrada(LocalDate.now().minusMonths(1));
        return vinculo;
    }

    private static AvisoLeitura leitura(Aviso aviso, Usuario usuario, Unidade unidade, LocalDateTime quando) {
        AvisoLeitura leitura = new AvisoLeitura();
        leitura.setAviso(aviso);
        leitura.setUsuario(usuario);
        leitura.setUnidade(unidade);
        leitura.setDataLeitura(quando);
        return leitura;
    }
}
