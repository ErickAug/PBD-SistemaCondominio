package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoReuniao;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReuniaoResumoDTO(Long id, String titulo, LocalDate data, LocalTime hora, String local,
                               SituacaoReuniao situacao, long itensPauta, long unidadesPresentes, boolean temAta) {
}
