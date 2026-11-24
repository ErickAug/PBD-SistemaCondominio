package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Reuniao;
import br.com.condominio.backend.model.ReuniaoPautaItem;
import br.com.condominio.backend.model.ReuniaoPresenca;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoReuniao;
import br.com.condominio.backend.repository.ReuniaoPautaItemRepository;
import br.com.condominio.backend.repository.ReuniaoPresencaRepository;
import br.com.condominio.backend.repository.ReuniaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReuniaoServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private ReuniaoRepository reuniaoRepository;
    @Mock
    private ReuniaoPautaItemRepository pautaRepository;
    @Mock
    private ReuniaoPresencaRepository presencaRepository;
    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private ReuniaoService service;

    private Condominio condominio;
    private Condominio outroCondominio;
    private Usuario sindico;
    private Unidade unidade101;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);
        outroCondominio = new Condominio();
        outroCondominio.setId(2L);

        sindico = new Usuario();
        sindico.setId(9L);
        sindico.setNome("Sindico");
        sindico.setPerfil(Perfil.SINDICO);

        Bloco bloco = new Bloco();
        bloco.setId(10L);
        bloco.setNome("A");
        bloco.setCondominio(condominio);

        unidade101 = new Unidade();
        unidade101.setId(101L);
        unidade101.setNumero("101");
        unidade101.setBloco(bloco);

        lenient().when(reuniaoRepository.save(any(Reuniao.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(presencaRepository.save(any(ReuniaoPresenca.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void convocacaoGuardaDataHoraLocalEPautaEmOrdem() {
        LocalDate data = LocalDate.now().plusDays(10);

        Reuniao r = service.convocar(condominio, sindico, " Assembleia ordinaria ", data, LocalTime.of(19, 30),
                " Salao de festas ", List.of(" Prestacao de contas ", "Eleicao do sindico", "Pintura da fachada"));

        assertEquals("Assembleia ordinaria", r.getTitulo());
        assertEquals(data, r.getData());
        assertEquals(LocalTime.of(19, 30), r.getHora());
        assertEquals("Salao de festas", r.getLocal());
        assertSame(condominio, r.getCondominio());

        ArgumentCaptor<List<ReuniaoPautaItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(pautaRepository).saveAll(captor.capture());
        List<ReuniaoPautaItem> itens = captor.getValue();
        assertEquals(List.of(1, 2, 3), itens.stream().map(ReuniaoPautaItem::getOrdem).toList());
        assertEquals(List.of("Prestacao de contas", "Eleicao do sindico", "Pintura da fachada"),
                itens.stream().map(ReuniaoPautaItem::getDescricao).toList());
        itens.forEach(i -> assertSame(r, i.getReuniao()));
    }

    @Test
    void convocacaoJaNascePublicadaParaOCondominio() {
        Reuniao r = service.convocar(condominio, sindico, "AGE", LocalDate.now().plusDays(5), LocalTime.of(20, 0),
                "Salao", List.of("Obra"));

        assertEquals(SituacaoReuniao.CONVOCADA, r.getSituacao());
        assertNotNull(r.getDataPublicacao());
        assertSame(sindico, r.getConvocadaPor());
    }

    @Test
    void convocacaoSemPautaEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, "AGE", LocalDate.now(), LocalTime.NOON, "Salao", List.of()));

        assertEquals("Informe a pauta com pelo menos um assunto.", erro.getMessage());
        verify(reuniaoRepository, never()).save(any());
    }

    @Test
    void itemDePautaEmBrancoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, "AGE", LocalDate.now(), LocalTime.NOON, "Salao",
                        Arrays.asList("Obra", " ")));

        assertEquals("Os itens da pauta não podem ficar em branco.", erro.getMessage());
    }

    @Test
    void convocacaoSemDataHoraLocalOuTituloEhRecusada() {
        assertEquals("Informe o título da reunião.", assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, " ", LocalDate.now(), LocalTime.NOON, "S", List.of("x")))
                .getMessage());
        assertEquals("Informe a data da reunião.", assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, "T", null, LocalTime.NOON, "S", List.of("x")))
                .getMessage());
        assertEquals("Informe a hora da reunião.", assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, "T", LocalDate.now(), null, "S", List.of("x")))
                .getMessage());
        assertEquals("Informe o local da reunião.", assertThrows(RegraDeNegocioException.class,
                () -> service.convocar(condominio, sindico, "T", LocalDate.now(), LocalTime.NOON, " ", List.of("x")))
                .getMessage());
    }

    @Test
    void presencaEhRegistradaPorUnidadeComQuemCompareceu() {
        Reuniao r = reuniao(5L, LocalDate.now(), LocalTime.of(8, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));
        when(unidadeRepository.findById(101L)).thenReturn(Optional.of(unidade101));
        when(presencaRepository.findByReuniaoIdAndUnidadeId(5L, 101L)).thenReturn(Optional.empty());

        ReuniaoPresenca p = service.registrarPresenca(CONDOMINIO_ID, 5L, 101L, " Maria (inquilina) ", sindico);

        assertSame(r, p.getReuniao());
        assertSame(unidade101, p.getUnidade());
        assertEquals("Maria (inquilina)", p.getCompareceu());
        assertSame(sindico, p.getRegistradoPor());
        assertNotNull(p.getDataRegistro());
    }

    @Test
    void mesmaUnidadeNaoTemDuasPresencas() {
        Reuniao r = reuniao(5L, LocalDate.now(), LocalTime.of(8, 0), SituacaoReuniao.CONVOCADA);
        ReuniaoPresenca existente = new ReuniaoPresenca();
        existente.setCompareceu("Joao");
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));
        when(unidadeRepository.findById(101L)).thenReturn(Optional.of(unidade101));
        when(presencaRepository.findByReuniaoIdAndUnidadeId(5L, 101L)).thenReturn(Optional.of(existente));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrarPresenca(CONDOMINIO_ID, 5L, 101L, "Maria", sindico));

        assertEquals("A unidade 101 do bloco A já tem presença registrada nesta reunião (compareceu: Joao).",
                erro.getMessage());
        verify(presencaRepository, never()).save(any());
    }

    @Test
    void presencaAntesDoDiaDaReuniaoEhRecusada() {
        Reuniao r = reuniao(5L, LocalDate.now().plusDays(3), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrarPresenca(CONDOMINIO_ID, 5L, 101L, "Maria", sindico));

        assertEquals("A presença só pode ser registrada a partir do dia da reunião.", erro.getMessage());
    }

    @Test
    void presencaDeUnidadeDeOutroCondominioEhRecusada() {
        Bloco blocoDeFora = new Bloco();
        blocoDeFora.setNome("X");
        blocoDeFora.setCondominio(outroCondominio);
        Unidade deFora = new Unidade();
        deFora.setId(900L);
        deFora.setBloco(blocoDeFora);
        Reuniao r = reuniao(5L, LocalDate.now(), LocalTime.of(8, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));
        when(unidadeRepository.findById(900L)).thenReturn(Optional.of(deFora));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrarPresenca(CONDOMINIO_ID, 5L, 900L, "Maria", sindico));

        assertEquals("Unidade não encontrada neste condomínio.", erro.getMessage());
    }

    @Test
    void presencaSemNomeDeQuemCompareceuEhRecusada() {
        Reuniao r = reuniao(5L, LocalDate.now(), LocalTime.of(8, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrarPresenca(CONDOMINIO_ID, 5L, 101L, " ", sindico));

        assertEquals("Informe quem compareceu pela unidade.", erro.getMessage());
    }

    @Test
    void presencaDepoisDaAtaEhRecusada() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(2), LocalTime.of(19, 0), SituacaoReuniao.REALIZADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrarPresenca(CONDOMINIO_ID, 5L, 101L, "Maria", sindico));

        assertEquals("A ata já foi anexada; a lista de presença não pode mais ser alterada.", erro.getMessage());
    }

    @Test
    void ataEhAnexadaDepoisDeRealizadaEAReuniaoFicaRealizada() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(1), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        Reuniao comAta = service.anexarAta(CONDOMINIO_ID, 5L, " Aprovada a pintura ",
                "data:application/pdf;base64,JVBERi0=", sindico);

        assertEquals(SituacaoReuniao.REALIZADA, comAta.getSituacao());
        assertEquals("Aprovada a pintura", comAta.getAtaTexto());
        assertEquals("data:application/pdf;base64,JVBERi0=", comAta.getAtaArquivo());
        assertSame(sindico, comAta.getAtaAnexadaPor());
        assertNotNull(comAta.getDataAnexoAta());
    }

    @Test
    void ataSemArquivoEhAceita() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(1), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        assertNull(service.anexarAta(CONDOMINIO_ID, 5L, "Deliberacoes", null, sindico).getAtaArquivo());
    }

    @Test
    void ataAntesDaReuniaoAcontecerEhRecusada() {
        Reuniao r = reuniao(5L, LocalDate.now().plusDays(1), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.anexarAta(CONDOMINIO_ID, 5L, "Deliberacoes", null, sindico));

        assertEquals("A ata só pode ser anexada depois que a reunião acontecer.", erro.getMessage());
        verify(reuniaoRepository, never()).save(any());
    }

    @Test
    void ataNaoEhAnexadaDuasVezes() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(1), LocalTime.of(19, 0), SituacaoReuniao.REALIZADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.anexarAta(CONDOMINIO_ID, 5L, "Outra", null, sindico));

        assertEquals("Esta reunião já tem ata anexada.", erro.getMessage());
    }

    @Test
    void ataSemTextoEhRecusada() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(1), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.anexarAta(CONDOMINIO_ID, 5L, " ", null, sindico));

        assertEquals("Informe o texto da ata com as deliberações.", erro.getMessage());
    }

    @Test
    void arquivoDaAtaForaDoFormatoEhRecusado() {
        Reuniao r = reuniao(5L, LocalDate.now().minusDays(1), LocalTime.of(19, 0), SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.of(r));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.anexarAta(CONDOMINIO_ID, 5L, "Texto", "ata.pdf", sindico));

        assertEquals("O arquivo da ata deve ser enviado em base64 no formato data:<tipo>;base64,...",
                erro.getMessage());
    }

    @Test
    void historicoFiltraPorPeriodo() {
        Reuniao janeiro = reuniao(1L, LocalDate.of(2026, 1, 15), LocalTime.NOON, SituacaoReuniao.REALIZADA);
        Reuniao marco = reuniao(2L, LocalDate.of(2026, 3, 10), LocalTime.NOON, SituacaoReuniao.REALIZADA);
        Reuniao junho = reuniao(3L, LocalDate.of(2026, 6, 1), LocalTime.NOON, SituacaoReuniao.CONVOCADA);
        when(reuniaoRepository.findByCondominioIdOrderByDataDescHoraDesc(CONDOMINIO_ID))
                .thenReturn(List.of(junho, marco, janeiro));

        assertEquals(List.of(marco, janeiro),
                service.listarHistorico(CONDOMINIO_ID, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10)));
        assertEquals(List.of(junho, marco), service.listarHistorico(CONDOMINIO_ID, LocalDate.of(2026, 2, 1), null));
        assertEquals(List.of(janeiro), service.listarHistorico(CONDOMINIO_ID, null, LocalDate.of(2026, 2, 1)));
        assertEquals(List.of(junho, marco, janeiro), service.listarHistorico(CONDOMINIO_ID, null, null));
    }

    @Test
    void periodoInvertidoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.listarHistorico(CONDOMINIO_ID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1)));

        assertEquals("A data inicial do período não pode ser depois da data final.", erro.getMessage());
    }

    @Test
    void reuniaoDeOutroCondominioNaoEhEncontrada() {
        when(reuniaoRepository.findByIdAndCondominioId(5L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.buscarNoCondominio(CONDOMINIO_ID, 5L));

        assertEquals("Reunião não encontrada.", erro.getMessage());
    }

    private Reuniao reuniao(Long id, LocalDate data, LocalTime hora, SituacaoReuniao situacao) {
        Reuniao r = new Reuniao();
        r.setId(id);
        r.setCondominio(condominio);
        r.setTitulo("Reuniao " + id);
        r.setData(data);
        r.setHora(hora);
        r.setLocal("Salao");
        r.setSituacao(situacao);
        return r;
    }
}
