package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoReserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservaResponseDTO(Long id, Long areaComumId, String areaComumNome, Long unidadeId,
                                 String blocoNome, String unidadeNumero, LocalDate data, LocalTime horaInicio,
                                 LocalTime horaFim, String finalidade, SituacaoReserva situacao,
                                 String criadoPorNome, LocalDateTime dataCriacao, String decididoPorNome,
                                 LocalDateTime dataDecisao, String motivoRecusa, String canceladoPorNome,
                                 LocalDateTime dataCancelamento) {
}
