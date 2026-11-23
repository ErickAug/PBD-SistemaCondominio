package br.com.condominio.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PassagemTurnoResponseDTO(Long id, String porteiroNome, LocalDateTime horarioEncerramento,
                                        List<OcorrenciaResponseDTO> pendencias) {
}
