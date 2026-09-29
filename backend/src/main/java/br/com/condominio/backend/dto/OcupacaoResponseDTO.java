package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoOcupacao;

import java.time.LocalDate;

public record OcupacaoResponseDTO(Long id, Long unidadeId, TipoOcupacao tipo,
                                  Long ocupanteId, String ocupanteNome, LocalDate dataEntrada) {
}