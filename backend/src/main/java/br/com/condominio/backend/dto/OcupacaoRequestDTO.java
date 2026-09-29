package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoOcupacao;

import java.time.LocalDate;

public record OcupacaoRequestDTO(TipoOcupacao tipo, Long ocupanteId, LocalDate dataEntrada) {
}