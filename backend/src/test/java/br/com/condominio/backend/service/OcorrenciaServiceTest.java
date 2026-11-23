package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.OcorrenciaRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do card T13 (Livro de ocorrências e passagem de turno).
 * Cobrem os critérios de aceite definidos para o OcorrenciaService.
 */
@ExtendWith(MockitoExtension.class)
class OcorrenciaServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;
    @Mock
    private CondominioRepository condominioRepository;
    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private OcorrenciaService ocorrenciaService;

    private Condominio condominio;
    private Usuario porteiro;

    @BeforeEach
    void setUp() {
        condominio = mock(Condominio.class);
        lenient().when(condominio.getId()).thenReturn(CONDOMINIO_ID);

        porteiro = mock(Usuario.class);
        lenient().when(porteiro.getNome()).thenReturn("Porteiro Teste");

        lenient().when(condominioRepository.findById(CONDOMINIO_ID)).thenReturn(Optional.of(condominio));
        lenient().when(ocorrenciaRepository.save(any(Ocorrencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Critério 1: anotação com texto, tipo, unidade (opcional), autor e data/hora

    @Test
    void registrarDeveCriarOcorrenciaComTextoTipoAutorEDataHora() {
        Ocorrencia ocorrencia = ocorrenciaService.registrar(CONDOMINIO_ID, "Barulho no 3º andar", "Barulho",
                null, porteiro);

        assertEquals("Barulho no 3º andar", ocorrencia.getTexto());
        assertEquals("Barulho", ocorrencia.getTipo());
        assertEquals(porteiro, ocorrencia.getAutor());
        assertNotNull(ocorrencia.getDataHora());
        assertNull(ocorrencia.getUnidade());
        assertFalse(ocorrencia.isResolvida());
    }

    @Test
    void registrarComUnidadeDoCondominioDeveVincularAUnidade() {
        Condominio mesmoCondominio = mock(Condominio.class);
        when(mesmoCondominio.getId()).thenReturn(CONDOMINIO_ID);
        Bloco bloco = mock(Bloco.class);
        when(bloco.getCondominio()).thenReturn(mesmoCondominio);
        Unidade unidade = mock(Unidade.class);
        when(unidade.getBloco()).thenReturn(bloco);
        when(unidadeRepository.findById(5L)).thenReturn(Optional.of(unidade));

        Ocorrencia ocorrencia = ocorrenciaService.registrar(CONDOMINIO_ID, "Veículo irregular", "Veículo",
                5L, porteiro);

        assertEquals(unidade, ocorrencia.getUnidade());
    }

    @Test
    void registrarComUnidadeDeOutroCondominioDeveLancarExcecao() {
        Condominio outroCondominio = mock(Condominio.class);
        when(outroCondominio.getId()).thenReturn(99L);
        Bloco bloco = mock(Bloco.class);
        when(bloco.getCondominio()).thenReturn(outroCondominio);
        Unidade unidade = mock(Unidade.class);
        when(unidade.getBloco()).thenReturn(bloco);
        when(unidadeRepository.findById(5L)).thenReturn(Optional.of(unidade));

        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.registrar(CONDOMINIO_ID, "Veículo irregular", "Veículo", 5L, porteiro));
    }

    @Test
    void registrarSemTextoDeveLancarExcecao() {
        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.registrar(CONDOMINIO_ID, " ", "Barulho", null, porteiro));
    }

    @Test
    void registrarSemTipoDeveLancarExcecao() {
        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.registrar(CONDOMINIO_ID, "Texto qualquer", null, null, porteiro));
    }

    // Critério 2: correção é anotação nova ligada à anterior, nunca edição da original

    @Test
    void corrigirDeveCriarNovaOcorrenciaLigadaAOriginal() {
        Ocorrencia original = new Ocorrencia();
        original.setTexto("Texto original");
        original.setTipo("Barulho");
        original.setCondominio(condominio);
        when(ocorrenciaRepository.buscarPorIdNoCondominio(10L, CONDOMINIO_ID)).thenReturn(Optional.of(original));

        Ocorrencia correcao = ocorrenciaService.corrigir(CONDOMINIO_ID, 10L, "Texto corrigido", porteiro);

        assertEquals("Texto corrigido", correcao.getTexto());
        assertEquals(original, correcao.getCorrecaoDe());
        assertEquals("Barulho", correcao.getTipo());
        assertEquals("Texto original", original.getTexto(), "a ocorrência original não deve ser alterada");
    }

    @Test
    void corrigirOcorrenciaInexistenteDeveLancarExcecao() {
        when(ocorrenciaRepository.buscarPorIdNoCondominio(999L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.corrigir(CONDOMINIO_ID, 999L, "Texto corrigido", porteiro));
    }

    @Test
    void corrigirSemTextoDeveLancarExcecao() {
        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.corrigir(CONDOMINIO_ID, 10L, "", porteiro));
    }

    // Critério 5: o livro filtra por período, unidade e tipo

    @Test
    void filtrarDeveConverterPeriodoParaInicioEFimDoDiaEDelegarAoRepositorio() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(ocorrenciaRepository.filtrarPorCondominio(eq(CONDOMINIO_ID), eq(inicio.atStartOfDay()),
                eq(fim.atTime(LocalTime.MAX)), eq("Barulho"), eq(5L)))
                .thenReturn(List.of(new Ocorrencia()));

        List<Ocorrencia> resultado = ocorrenciaService.filtrar(CONDOMINIO_ID, inicio, fim, "Barulho", 5L);

        assertEquals(1, resultado.size());
    }

    @Test
    void filtrarSemFiltrosDeveRepassarNulosAoRepositorio() {
        when(ocorrenciaRepository.filtrarPorCondominio(eq(CONDOMINIO_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(new Ocorrencia(), new Ocorrencia()));

        List<Ocorrencia> resultado = ocorrenciaService.filtrar(CONDOMINIO_ID, null, null, null, null);

        assertEquals(2, resultado.size());
    }

    // Critério 7: resolver guarda quem resolveu e quando

    @Test
    void marcarResolvidaDeveGuardarResponsavelEDataHora() {
        Ocorrencia ocorrencia = new Ocorrencia();
        when(ocorrenciaRepository.buscarPorIdNoCondominio(10L, CONDOMINIO_ID)).thenReturn(Optional.of(ocorrencia));

        Usuario sindico = mock(Usuario.class);

        Ocorrencia resolvida = ocorrenciaService.marcarResolvida(CONDOMINIO_ID, 10L, sindico);

        assertTrue(resolvida.isResolvida());
        assertEquals(sindico, resolvida.getResolvidoPor());
        assertNotNull(resolvida.getResolvidoEm());
    }

    @Test
    void marcarResolvidaDeOcorrenciaJaResolvidaDeveLancarExcecao() {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setResolvida(true);
        when(ocorrenciaRepository.buscarPorIdNoCondominio(10L, CONDOMINIO_ID)).thenReturn(Optional.of(ocorrencia));

        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.marcarResolvida(CONDOMINIO_ID, 10L, porteiro));
    }

    @Test
    void marcarResolvidaDeOcorrenciaInexistenteDeveLancarExcecao() {
        when(ocorrenciaRepository.buscarPorIdNoCondominio(999L, CONDOMINIO_ID)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class,
                () -> ocorrenciaService.marcarResolvida(CONDOMINIO_ID, 999L, porteiro));
    }

    // Critério 3 (apoio): pendências são as ocorrências ainda não resolvidas do condomínio

    @Test
    void listarPendentesDeveDelegarAoRepositorio() {
        when(ocorrenciaRepository.listarPendentesPorCondominio(CONDOMINIO_ID))
                .thenReturn(List.of(new Ocorrencia(), new Ocorrencia()));

        List<Ocorrencia> pendentes = ocorrenciaService.listarPendentes(CONDOMINIO_ID);

        assertEquals(2, pendentes.size());
    }
}
