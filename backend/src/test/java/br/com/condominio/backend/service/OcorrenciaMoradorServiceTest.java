package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.OcorrenciaMoradorHistorico;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;
import br.com.condominio.backend.repository.OcorrenciaMoradorHistoricoRepository;
import br.com.condominio.backend.repository.OcorrenciaMoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcorrenciaMoradorServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private OcorrenciaMoradorRepository ocorrenciaMoradorRepository;
    @Mock
    private OcorrenciaMoradorHistoricoRepository historicoRepository;
    @Mock
    private MoradiaService moradiaService;

    @InjectMocks
    private OcorrenciaMoradorService service;

    private Condominio condominio;
    private Unidade unidade;
    private Usuario inquilino;
    private Usuario outroMorador;
    private Usuario donoQueNaoMora;
    private Usuario estranho;
    private Usuario sindico;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);

        Bloco bloco = new Bloco();
        bloco.setId(10L);
        bloco.setNome("A");
        bloco.setCondominio(condominio);

        inquilino = usuario(1L, "Inquilino", Perfil.MORADOR);
        outroMorador = usuario(2L, "Familiar", Perfil.MORADOR);
        donoQueNaoMora = usuario(3L, "Dono", Perfil.PROPRIETARIO);
        estranho = usuario(4L, "Morador de outra unidade", Perfil.MORADOR);
        sindico = usuario(5L, "Sindico", Perfil.SINDICO);

        unidade = new Unidade();
        unidade.setId(101L);
        unidade.setNumero("101");
        unidade.setBloco(bloco);
        unidade.setProprietario(donoQueNaoMora);

        lenient().when(moradiaService.moraNaUnidade(any(Usuario.class), any())).thenReturn(false);
        lenient().when(moradiaService.moraNaUnidade(inquilino, 101L)).thenReturn(true);
        lenient().when(moradiaService.moraNaUnidade(outroMorador, 101L)).thenReturn(true);
        lenient().when(ocorrenciaMoradorRepository.save(any(OcorrenciaMorador.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historicoRepository.save(any(OcorrenciaMoradorHistorico.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void abrirGuardaAssuntoTipoDescricaoEFoto() {
        OcorrenciaMorador o = service.abrir(unidade, inquilino, " Infiltracao ", TipoOcorrenciaMorador.DEFEITO,
                " Mancha no teto do banheiro ", "data:image/png;base64,AAAA");

        assertEquals("Infiltracao", o.getAssunto());
        assertEquals(TipoOcorrenciaMorador.DEFEITO, o.getTipo());
        assertEquals("Mancha no teto do banheiro", o.getDescricao());
        assertEquals("data:image/png;base64,AAAA", o.getFoto());
        assertSame(unidade, o.getUnidade());
        assertSame(condominio, o.getCondominio());
    }

    @Test
    void abrirSemFotoEhAceito() {
        OcorrenciaMorador o = service.abrir(unidade, inquilino, "Barulho", TipoOcorrenciaMorador.RECLAMACAO,
                "Som alto depois das 22h", null);

        assertNull(o.getFoto());
    }

    @Test
    void fotoEmBrancoViraSemFoto() {
        OcorrenciaMorador o = service.abrir(unidade, inquilino, "Barulho", TipoOcorrenciaMorador.RECLAMACAO,
                "Som alto", "   ");

        assertNull(o.getFoto());
    }

    @Test
    void fotoQueNaoEhImagemEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, inquilino, "A", TipoOcorrenciaMorador.DEFEITO, "D", "http://x/foto.png"));

        assertEquals("A foto deve ser uma imagem em base64 no formato data:image/...", erro.getMessage());
    }

    @Test
    void fotoGrandeDemaisEhRecusada() {
        String grande = "data:image/png;base64," + "A".repeat(OcorrenciaMoradorService.TAMANHO_MAXIMO_FOTO);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, inquilino, "A", TipoOcorrenciaMorador.DEFEITO, "D", grande));

        assertEquals("A foto é grande demais. O limite é de cerca de 2 MB.", erro.getMessage());
    }

    @Test
    void abrirSemAssuntoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, inquilino, " ", TipoOcorrenciaMorador.DEFEITO, "D", null));

        assertEquals("Informe o assunto da ocorrência.", erro.getMessage());
    }

    @Test
    void abrirSemTipoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, inquilino, "A", null, "D", null));

        assertEquals("Informe o tipo da ocorrência: RECLAMACAO, SOLICITACAO ou DEFEITO.", erro.getMessage());
    }

    @Test
    void abrirSemDescricaoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, inquilino, "A", TipoOcorrenciaMorador.DEFEITO, null, null));

        assertEquals("Informe a descrição da ocorrência.", erro.getMessage());
        verify(ocorrenciaMoradorRepository, never()).save(any());
    }

    @Test
    void ocorrenciaNasceAbertaComQuemAbriuEDataENoHistorico() {
        OcorrenciaMorador o = service.abrir(unidade, inquilino, "Lampada", TipoOcorrenciaMorador.SOLICITACAO,
                "Trocar lampada do corredor", null);

        assertEquals(SituacaoOcorrenciaMorador.ABERTA, o.getSituacao());
        assertSame(inquilino, o.getAbertoPor());
        assertNotNull(o.getDataAbertura());
        assertNull(o.getRespondidoPor());

        ArgumentCaptor<OcorrenciaMoradorHistorico> captor = ArgumentCaptor.forClass(OcorrenciaMoradorHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertNull(captor.getValue().getSituacaoAnterior());
        assertEquals(SituacaoOcorrenciaMorador.ABERTA, captor.getValue().getSituacaoNova());
        assertSame(inquilino, captor.getValue().getResponsavel());
        assertEquals(o.getDataAbertura(), captor.getValue().getDataHora());
    }

    @Test
    void proprietarioQueNaoMoraPodeAbrirOcorrenciaDaSuaUnidade() {
        OcorrenciaMorador o = service.abrir(unidade, donoQueNaoMora, "Telhado", TipoOcorrenciaMorador.DEFEITO,
                "Goteira", null);

        assertSame(donoQueNaoMora, o.getAbertoPor());
    }

    @Test
    void quemNaoMoraNemEhDonoNaoAbreOcorrencia() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.abrir(unidade, estranho, "A", TipoOcorrenciaMorador.DEFEITO, "D", null));

        assertEquals("Acesso negado a esta unidade.", erro.getMessage());
        verify(ocorrenciaMoradorRepository, never()).save(any());
    }

    @Test
    void quemMoraVeAsOcorrenciasDaUnidade() {
        List<OcorrenciaMorador> daUnidade = List.of(ocorrencia(1L, SituacaoOcorrenciaMorador.ABERTA, outroMorador));
        when(ocorrenciaMoradorRepository.findByUnidadeIdOrderByDataAberturaDesc(101L)).thenReturn(daUnidade);

        assertEquals(daUnidade, service.listarDaUnidade(unidade, inquilino));
    }

    @Test
    void proprietarioQueNaoMoraVeSoAsQueEleAbriu() {
        List<OcorrenciaMorador> dele = List.of(ocorrencia(2L, SituacaoOcorrenciaMorador.ABERTA, donoQueNaoMora));
        when(ocorrenciaMoradorRepository.findByUnidadeIdAndAbertoPorIdOrderByDataAberturaDesc(101L, 3L))
                .thenReturn(dele);

        assertEquals(dele, service.listarDaUnidade(unidade, donoQueNaoMora));
        verify(ocorrenciaMoradorRepository, never()).findByUnidadeIdOrderByDataAberturaDesc(any());
    }

    @Test
    void moradorDeOutraUnidadeNaoListaAsOcorrenciasDestaUnidade() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.listarDaUnidade(unidade, estranho));

        assertEquals("Acesso negado a esta unidade.", erro.getMessage());
    }

    @Test
    void detalheDeOcorrenciaDeOutraUnidadeNaoEhEncontrado() {
        when(ocorrenciaMoradorRepository.findByIdAndUnidadeId(99L, 101L)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.detalharDaUnidade(unidade, inquilino, 99L));

        assertEquals("Ocorrência não encontrada.", erro.getMessage());
    }

    @Test
    void proprietarioQueNaoMoraNaoVeOcorrenciaAbertaPeloInquilino() {
        OcorrenciaMorador doInquilino = ocorrencia(7L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndUnidadeId(7L, 101L)).thenReturn(Optional.of(doInquilino));

        assertThrows(RegraDeNegocioException.class, () -> service.detalharDaUnidade(unidade, donoQueNaoMora, 7L));
    }

    @Test
    void moradorVeODetalheDaOcorrenciaDaUnidade() {
        OcorrenciaMorador o = ocorrencia(8L, SituacaoOcorrenciaMorador.EM_ANALISE, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndUnidadeId(8L, 101L)).thenReturn(Optional.of(o));

        assertSame(o, service.detalharDaUnidade(unidade, inquilino, 8L));
    }

    @Test
    void sindicoRespondeMudandoASituacaoERegistraQuemEQuando() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        OcorrenciaMorador respondida = service.responder(CONDOMINIO_ID, 9L, sindico,
                SituacaoOcorrenciaMorador.EM_ANALISE, " Vamos chamar o eletricista ");

        assertEquals(SituacaoOcorrenciaMorador.EM_ANALISE, respondida.getSituacao());
        assertSame(sindico, respondida.getRespondidoPor());
        assertEquals("Vamos chamar o eletricista", respondida.getUltimaResposta());
        assertNotNull(respondida.getDataUltimaResposta());

        ArgumentCaptor<OcorrenciaMoradorHistorico> captor = ArgumentCaptor.forClass(OcorrenciaMoradorHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertEquals(SituacaoOcorrenciaMorador.ABERTA, captor.getValue().getSituacaoAnterior());
        assertEquals(SituacaoOcorrenciaMorador.EM_ANALISE, captor.getValue().getSituacaoNova());
        assertEquals("Vamos chamar o eletricista", captor.getValue().getResposta());
        assertSame(sindico, captor.getValue().getResponsavel());
    }

    @Test
    void mudarSituacaoSemTextoDeRespostaEhRecusado() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.RESOLVIDA, "  "));

        assertEquals("Informe o texto de resposta para o morador.", erro.getMessage());
        assertEquals(SituacaoOcorrenciaMorador.ABERTA, o.getSituacao());
        verify(historicoRepository, never()).save(any());
    }

    @Test
    void responderSemSituacaoEhRecusado() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, null, "Texto"));

        assertEquals("Informe a nova situação: EM_ANALISE, RESOLVIDA ou RECUSADA.", erro.getMessage());
    }

    @Test
    void naoVoltaParaAberta() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.EM_ANALISE, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.ABERTA, "Texto"));

        assertEquals("Uma ocorrência não pode voltar para a situação ABERTA.", erro.getMessage());
    }

    @Test
    void mesmaSituacaoEhRecusada() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.EM_ANALISE, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.EM_ANALISE, "Texto"));

        assertEquals("A ocorrência já está nesta situação.", erro.getMessage());
    }

    @Test
    void ocorrenciaEncerradaNaoMudaMais() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.RECUSADA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.RESOLVIDA, "Texto"));

        assertEquals("Esta ocorrência já foi encerrada e não pode mudar de situação.", erro.getMessage());
    }

    @Test
    void responderOcorrenciaDeOutroCondominioNaoEncontra() {
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.RESOLVIDA, "Texto"));

        assertEquals("Ocorrência não encontrada.", erro.getMessage());
    }

    @Test
    void duasRespostasFicamNoHistorico() {
        OcorrenciaMorador o = ocorrencia(9L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        when(ocorrenciaMoradorRepository.findByIdAndCondominioId(9L, CONDOMINIO_ID)).thenReturn(Optional.of(o));

        service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.EM_ANALISE, "Analisando");
        service.responder(CONDOMINIO_ID, 9L, sindico, SituacaoOcorrenciaMorador.RESOLVIDA, "Resolvido");

        verify(historicoRepository, times(2)).save(any(OcorrenciaMoradorHistorico.class));
        assertEquals(SituacaoOcorrenciaMorador.RESOLVIDA, o.getSituacao());
        assertEquals("Resolvido", o.getUltimaResposta());
        assertTrue(service.estaEncerrada(o));
    }

    @Test
    void filaTrazAbertasPrimeiroDepoisEmAnaliseEPorUltimoEncerradas() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 8, 0);
        OcorrenciaMorador resolvidaAntiga = ocorrencia(1L, SituacaoOcorrenciaMorador.RESOLVIDA, inquilino, base);
        OcorrenciaMorador emAnalise = ocorrencia(2L, SituacaoOcorrenciaMorador.EM_ANALISE, inquilino, base.plusDays(1));
        OcorrenciaMorador abertaNova = ocorrencia(3L, SituacaoOcorrenciaMorador.ABERTA, inquilino, base.plusDays(3));
        OcorrenciaMorador abertaAntiga = ocorrencia(4L, SituacaoOcorrenciaMorador.ABERTA, inquilino, base.plusDays(2));
        OcorrenciaMorador recusadaNova = ocorrencia(5L, SituacaoOcorrenciaMorador.RECUSADA, inquilino, base.plusDays(4));
        when(ocorrenciaMoradorRepository.findByCondominioId(CONDOMINIO_ID))
                .thenReturn(List.of(resolvidaAntiga, emAnalise, abertaNova, abertaAntiga, recusadaNova));

        List<Long> ordem = service.listarFila(CONDOMINIO_ID, null).stream().map(OcorrenciaMorador::getId).toList();

        assertEquals(List.of(4L, 3L, 2L, 5L, 1L), ordem);
    }

    @Test
    void filaFiltraPorSituacao() {
        OcorrenciaMorador aberta = ocorrencia(1L, SituacaoOcorrenciaMorador.ABERTA, inquilino);
        OcorrenciaMorador resolvida = ocorrencia(2L, SituacaoOcorrenciaMorador.RESOLVIDA, inquilino);
        when(ocorrenciaMoradorRepository.findByCondominioId(CONDOMINIO_ID)).thenReturn(List.of(aberta, resolvida));

        assertEquals(List.of(resolvida), service.listarFila(CONDOMINIO_ID, SituacaoOcorrenciaMorador.RESOLVIDA));
    }

    @Test
    void estaEncerradaSoParaResolvidaOuRecusada() {
        assertFalse(service.estaEncerrada(ocorrencia(1L, SituacaoOcorrenciaMorador.ABERTA, inquilino)));
        assertFalse(service.estaEncerrada(ocorrencia(1L, SituacaoOcorrenciaMorador.EM_ANALISE, inquilino)));
        assertTrue(service.estaEncerrada(ocorrencia(1L, SituacaoOcorrenciaMorador.RESOLVIDA, inquilino)));
        assertTrue(service.estaEncerrada(ocorrencia(1L, SituacaoOcorrenciaMorador.RECUSADA, inquilino)));
    }

    private OcorrenciaMorador ocorrencia(Long id, SituacaoOcorrenciaMorador situacao, Usuario autor) {
        return ocorrencia(id, situacao, autor, LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    private OcorrenciaMorador ocorrencia(Long id, SituacaoOcorrenciaMorador situacao, Usuario autor,
                                         LocalDateTime abertura) {
        OcorrenciaMorador o = new OcorrenciaMorador();
        o.setId(id);
        o.setCondominio(condominio);
        o.setUnidade(unidade);
        o.setAssunto("Assunto " + id);
        o.setTipo(TipoOcorrenciaMorador.RECLAMACAO);
        o.setDescricao("Descricao " + id);
        o.setSituacao(situacao);
        o.setAbertoPor(autor);
        o.setDataAbertura(abertura);
        return o;
    }

    private static Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setUsuario("u" + id);
        u.setPerfil(perfil);
        return u;
    }
}
