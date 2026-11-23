package br.com.condominio.backend.service;

import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.model.PassagemTurno;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.OcorrenciaRepository;
import br.com.condominio.backend.repository.PassagemTurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassagemTurnoServiceTest {

    private static final Long CONDOMINIO_ID = 1L;

    @Mock
    private PassagemTurnoRepository passagemTurnoRepository;
    @Mock
    private OcorrenciaRepository ocorrenciaRepository;
    @Mock
    private CondominioRepository condominioRepository;

    @InjectMocks
    private PassagemTurnoService passagemTurnoService;

    private Condominio condominio;
    private Usuario porteiro;

    @BeforeEach
    void setUp() {
        condominio = org.mockito.Mockito.mock(Condominio.class);
        porteiro = org.mockito.Mockito.mock(Usuario.class);

        lenient().when(condominioRepository.findById(CONDOMINIO_ID)).thenReturn(Optional.of(condominio));
        lenient().when(passagemTurnoRepository.save(any(PassagemTurno.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }


    @Test
    void encerrarTurnoDeveRegistrarPorteiroHorarioEPendencias() {
        List<Ocorrencia> abertas = List.of(new Ocorrencia(), new Ocorrencia());
        when(ocorrenciaRepository.listarPendentesPorCondominio(CONDOMINIO_ID)).thenReturn(abertas);

        PassagemTurno turno = passagemTurnoService.encerrarTurno(CONDOMINIO_ID, porteiro);

        assertEquals(porteiro, turno.getPorteiro());
        assertNotNull(turno.getHorarioEncerramento());
        assertEquals(2, turno.getPendencias().size());
    }

    @Test
    void encerrarTurnoSemPendenciasDeveRegistrarListaVazia() {
        when(ocorrenciaRepository.listarPendentesPorCondominio(CONDOMINIO_ID)).thenReturn(List.of());

        PassagemTurno turno = passagemTurnoService.encerrarTurno(CONDOMINIO_ID, porteiro);

        assertTrue(turno.getPendencias().isEmpty());
    }


    @Test
    void buscarUltimoTurnoDeveDelegarAoRepositorio() {
        PassagemTurno ultimoTurno = new PassagemTurno();
        when(passagemTurnoRepository.findTopByCondominioIdOrderByHorarioEncerramentoDesc(CONDOMINIO_ID))
                .thenReturn(Optional.of(ultimoTurno));

        Optional<PassagemTurno> resultado = passagemTurnoService.buscarUltimoTurno(CONDOMINIO_ID);

        assertTrue(resultado.isPresent());
        assertEquals(ultimoTurno, resultado.get());
    }

    @Test
    void buscarUltimoTurnoQuandoNaoHaTurnoEncerradoDeveRetornarVazio() {
        when(passagemTurnoRepository.findTopByCondominioIdOrderByHorarioEncerramentoDesc(CONDOMINIO_ID))
                .thenReturn(Optional.empty());

        Optional<PassagemTurno> resultado = passagemTurnoService.buscarUltimoTurno(CONDOMINIO_ID);

        assertTrue(resultado.isEmpty());
    }
}
