package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AutorizacaoVisita;
import br.com.condominio.backend.model.AutorizadoPermanente;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.RegistroAcesso;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.model.enums.OrigemRegistroAcesso;
import br.com.condominio.backend.repository.AutorizacaoVisitaRepository;
import br.com.condominio.backend.repository.AutorizadoPermanenteRepository;
import br.com.condominio.backend.repository.RegistroAcessoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do card T11 (Registro de entrada).
 * Cobrem os sete critérios de aceite definidos para a portaria.
 */
@ExtendWith(MockitoExtension.class)
class RegistroAcessoServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private RegistroAcessoRepository registroAcessoRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private VeiculoService veiculoService;
    @Mock
    private AutorizacaoVisitaRepository autorizacaoVisitaRepository;
    @Mock
    private AutorizacaoVisitaService autorizacaoVisitaService;
    @Mock
    private AutorizadoPermanenteRepository autorizadoPermanenteRepository;
    @Mock
    private AutorizadoPermanenteService autorizadoPermanenteService;

    @InjectMocks
    private RegistroAcessoService registroAcessoService;

    private Usuario porteiro;
    private Unidade unidade;

    @BeforeEach
    void setUp() {
        // lenient(): este fixture é compartilhado por testes que não usam todos os stubs
        // (por exemplo, o fluxo "entrada por placa" nunca chega a consultar o condomínio da unidade).
        porteiro = mock(Usuario.class);
        lenient().when(porteiro.getNome()).thenReturn("Sindico Teste");

        Condominio condominio = mock(Condominio.class);
        lenient().when(condominio.getId()).thenReturn(CONDOMINIO_ID);

        Bloco bloco = mock(Bloco.class);
        lenient().when(bloco.getCondominio()).thenReturn(condominio);

        unidade = mock(Unidade.class);
        lenient().when(unidade.getBloco()).thenReturn(bloco);

        lenient().when(registroAcessoRepository.save(any(RegistroAcesso.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static <T> T mock(Class<T> tipo) {
        return org.mockito.Mockito.mock(tipo);
    }

    // Critério 1: registro manual com nome, documento, unidade, tipo, quem autorizou e horário
    @Test
    void registrarManualDeveCriarRegistroComDadosCompletosEHoraDeEntrada() {
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "11122233344"))
                .thenReturn(false);

        RegistroAcesso registro = registroAcessoService.registrarManual(CONDOMINIO_ID, 10L,
                "Carlos Entregador", "11122233344", "Entregador", "João (morador 101)", null, porteiro);

        assertEquals("Carlos Entregador", registro.getNome());
        assertEquals("11122233344", registro.getDocumento());
        assertEquals("Entregador", registro.getTipo());
        assertEquals("João (morador 101)", registro.getQuemAutorizou());
        assertEquals(unidade, registro.getUnidade());
        assertEquals(OrigemRegistroAcesso.MANUAL, registro.getOrigem());
        assertEquals(porteiro, registro.getPorteiro());
        assertNotNull(registro.getHoraEntrada());
        assertEquals(porteiro.getNome(), registro.getPorteiro().getNome());

        verify(registroAcessoRepository).save(any(RegistroAcesso.class));
    }

    // Critério 3: sem autorização prévia, exige quem liberou a entrada
    @Test
    void registrarManualSemQuemAutorizouDeveLancarExcecao() {
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarManual(CONDOMINIO_ID, 10L,
                        "Carlos Entregador", "11122233344", "Entregador", null, null, porteiro));

        assertEquals("Sem autorização prévia, registre quem, na unidade, liberou a entrada.", ex.getMessage());
        verify(registroAcessoRepository, never()).save(any());
    }

    @Test
    void registrarManualComQuemAutorizouEmBrancoDeveLancarExcecao() {
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));

        assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarManual(CONDOMINIO_ID, 10L,
                        "Carlos Entregador", "11122233344", "Entregador", "   ", null, porteiro));
    }

    // Critério 5: entrada de veículo aceita a placa, que já traz a unidade
    @Test
    void registrarManualComPlacaCadastradaDeveReaproveitarUnidadeDoVeiculo() {
        Veiculo veiculo = mock(Veiculo.class);
        when(veiculo.getUnidade()).thenReturn(unidade);
        when(veiculoService.buscarPorPlaca(CONDOMINIO_ID, "BRA1C23")).thenReturn(Optional.of(veiculo));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "99988877766"))
                .thenReturn(false);

        RegistroAcesso registro = registroAcessoService.registrarManual(CONDOMINIO_ID, null,
                "Pedro Motorista", "99988877766", "Visitante", "Autorizado pelo porteiro", "BRA1C23", porteiro);

        assertEquals(unidade, registro.getUnidade());
        assertEquals(veiculo, registro.getVeiculo());
        verify(unidadeRepository, never()).findById(anyLong());
    }

    @Test
    void registrarManualSemUnidadeNemPlacaDeveLancarExcecao() {
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarManual(CONDOMINIO_ID, null,
                        "Pedro Motorista", "99988877766", "Visitante", "Autorizado", null, porteiro));

        assertEquals("Informe a unidade ou uma placa já cadastrada.", ex.getMessage());
    }

    @Test
    void registrarManualComUnidadeDeOutroCondominioDeveLancarExcecao() {
        Condominio outroCondominio = mock(Condominio.class);
        when(outroCondominio.getId()).thenReturn(2L);
        Bloco blocoDeOutroCondominio = mock(Bloco.class);
        when(blocoDeOutroCondominio.getCondominio()).thenReturn(outroCondominio);
        Unidade unidadeDeOutroCondominio = mock(Unidade.class);
        when(unidadeDeOutroCondominio.getBloco()).thenReturn(blocoDeOutroCondominio);
        when(unidadeRepository.findById(99L)).thenReturn(Optional.of(unidadeDeOutroCondominio));

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarManual(CONDOMINIO_ID, 99L,
                        "Carlos Entregador", "11122233344", "Entregador", "Síndico", null, porteiro));

        assertEquals("Esta unidade não pertence a este condomínio.", ex.getMessage());
    }

    // Critério 6: não é possível registrar entrada de quem já está dentro sem ter saído
    @Test
    void registrarManualDePessoaJaDentroDeveLancarExcecao() {
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "11122233344"))
                .thenReturn(true);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarManual(CONDOMINIO_ID, 10L,
                        "Carlos Entregador", "11122233344", "Entregador", "João (morador 101)", null, porteiro));

        assertEquals("Esta pessoa já está dentro do condomínio e ainda não teve saída registrada.", ex.getMessage());
        verify(registroAcessoRepository, never()).save(any());
    }

    // Critério 2: entrada a partir de autorização prévia preenche os dados automaticamente
    @Test
    void registrarPorAutorizacaoVisitaDevePreencherDadosAutomaticamenteEConfirmarUso() {
        AutorizacaoVisita autorizacao = mock(AutorizacaoVisita.class);
        when(autorizacao.getUnidade()).thenReturn(unidade);
        when(autorizacao.getNome()).thenReturn("Ana Visitante");
        when(autorizacao.getDocumento()).thenReturn("99988877766");
        when(autorizacao.getTipo()).thenReturn("Visitante");
        when(autorizacaoVisitaRepository.findById(5L)).thenReturn(Optional.of(autorizacao));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "99988877766"))
                .thenReturn(false);

        RegistroAcesso registro = registroAcessoService.registrarPorAutorizacaoVisita(CONDOMINIO_ID, 5L, porteiro);

        assertEquals("Ana Visitante", registro.getNome());
        assertEquals("99988877766", registro.getDocumento());
        assertEquals(unidade, registro.getUnidade());
        assertEquals(OrigemRegistroAcesso.AUTORIZACAO_VISITA, registro.getOrigem());
        assertEquals("Autorização prévia cadastrada pelo morador", registro.getQuemAutorizou());
        assertEquals(autorizacao, registro.getAutorizacaoVisita());

        verify(autorizacaoVisitaService).confirmarUso(CONDOMINIO_ID, 5L);
    }

    @Test
    void registrarPorAutorizacaoVisitaInexistenteDeveLancarExcecao() {
        when(autorizacaoVisitaRepository.findById(404L)).thenReturn(Optional.empty());

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarPorAutorizacaoVisita(CONDOMINIO_ID, 404L, porteiro));

        assertEquals("Autorização não encontrada.", ex.getMessage());
        verify(autorizacaoVisitaService, never()).confirmarUso(anyLong(), anyLong());
    }

    @Test
    void registrarPorAutorizacaoVisitaDePessoaJaDentroNaoDeveConfirmarUso() {
        AutorizacaoVisita autorizacao = mock(AutorizacaoVisita.class);
        when(autorizacao.getDocumento()).thenReturn("99988877766");
        when(autorizacaoVisitaRepository.findById(5L)).thenReturn(Optional.of(autorizacao));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "99988877766"))
                .thenReturn(true);

        assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarPorAutorizacaoVisita(CONDOMINIO_ID, 5L, porteiro));

        verify(autorizacaoVisitaService, never()).confirmarUso(anyLong(), anyLong());
    }

    // Critério 4: autorizado permanente entra sem autorização nova, e o registro diz isso
    @Test
    void registrarPorAutorizadoPermanenteDeveCriarRegistroSemNovaAutorizacao() {
        AutorizadoPermanente autorizado = mock(AutorizadoPermanente.class);
        when(autorizado.getUnidade()).thenReturn(unidade);
        when(autorizado.getNome()).thenReturn("Diarista Joana");
        when(autorizado.getDocumento()).thenReturn("55566677788");
        when(autorizado.getTipo()).thenReturn("Diarista");
        when(autorizadoPermanenteRepository.findById(7L)).thenReturn(Optional.of(autorizado));
        when(autorizadoPermanenteService.estaLiberadoHoje(autorizado)).thenReturn(true);
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(CONDOMINIO_ID, "55566677788"))
                .thenReturn(false);

        RegistroAcesso registro = registroAcessoService.registrarPorAutorizadoPermanente(CONDOMINIO_ID, 7L, porteiro);

        assertEquals("Diarista Joana", registro.getNome());
        assertEquals(OrigemRegistroAcesso.AUTORIZADO_PERMANENTE, registro.getOrigem());
        assertEquals("Autorizado permanente", registro.getQuemAutorizou());
        assertEquals(autorizado, registro.getAutorizadoPermanente());
    }

    @Test
    void registrarPorAutorizadoPermanenteDeOutroCondominioDeveLancarExcecao() {
        Condominio outroCondominio = mock(Condominio.class);
        when(outroCondominio.getId()).thenReturn(2L);
        Bloco blocoDeOutroCondominio = mock(Bloco.class);
        when(blocoDeOutroCondominio.getCondominio()).thenReturn(outroCondominio);
        Unidade unidadeDeOutroCondominio = mock(Unidade.class);
        when(unidadeDeOutroCondominio.getBloco()).thenReturn(blocoDeOutroCondominio);
        AutorizadoPermanente autorizado = mock(AutorizadoPermanente.class);
        when(autorizado.getUnidade()).thenReturn(unidadeDeOutroCondominio);
        when(autorizadoPermanenteRepository.findById(7L)).thenReturn(Optional.of(autorizado));

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarPorAutorizadoPermanente(CONDOMINIO_ID, 7L, porteiro));

        assertEquals("Autorizado permanente não encontrado.", ex.getMessage());
    }

    @Test
    void registrarPorAutorizadoPermanenteForaDoPeriodoDeveLancarExcecao() {
        AutorizadoPermanente autorizado = mock(AutorizadoPermanente.class);
        when(autorizado.getUnidade()).thenReturn(unidade);
        when(autorizadoPermanenteRepository.findById(7L)).thenReturn(Optional.of(autorizado));
        when(autorizadoPermanenteService.estaLiberadoHoje(autorizado)).thenReturn(false);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> registroAcessoService.registrarPorAutorizadoPermanente(CONDOMINIO_ID, 7L, porteiro));

        assertEquals("Esta autorização permanente não é válida hoje.", ex.getMessage());
    }

    // Critério 7: o registro guarda qual porteiro fez o lançamento
    @Test
    void todosOsFluxosDeRegistroDevemGuardarOPorteiroResponsavel() {
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(registroAcessoRepository.existeEntradaAbertaPorDocumento(anyLong(), anyString())).thenReturn(false);

        RegistroAcesso registro = registroAcessoService.registrarManual(CONDOMINIO_ID, 10L,
                "Carlos Entregador", "11122233344", "Entregador", "João (morador 101)", null, porteiro);

        ArgumentCaptor<RegistroAcesso> captor = ArgumentCaptor.forClass(RegistroAcesso.class);
        verify(registroAcessoRepository).save(captor.capture());
        assertEquals(porteiro, captor.getValue().getPorteiro());
        assertEquals("Sindico Teste", registro.getPorteiro().getNome());
    }

    @Test
    void listarPorCondominioDeveDelegarAoRepositorio() {
        RegistroAcesso registro = new RegistroAcesso();
        when(registroAcessoRepository.listarPorCondominio(CONDOMINIO_ID)).thenReturn(List.of(registro));

        List<RegistroAcesso> resultado = registroAcessoService.listarPorCondominio(CONDOMINIO_ID);

        assertEquals(1, resultado.size());
        assertEquals(registro, resultado.get(0));
    }
}
