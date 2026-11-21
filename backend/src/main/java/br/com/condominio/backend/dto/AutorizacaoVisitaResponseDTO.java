package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.StatusAutorizacaoVisita;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AutorizacaoVisitaResponseDTO(Long id, Long unidadeId, String nome, String documento, String tipo,
                                            LocalDate dataInicio, LocalDate dataFim,
                                            StatusAutorizacaoVisita status, LocalDateTime dataUso,
                                            String situacao) {
}
