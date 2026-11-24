package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.AlcanceAviso;

import java.time.LocalDate;

public record AvisoRequestDTO(String titulo, String texto, AlcanceAviso alcance,
                              Long blocoId, Long unidadeId, LocalDate dataValidade) {
}
