package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoOcupacao;

import java.math.BigDecimal;

public record UnidadeResponseDTO(Long id, String numero, Integer andar, BigDecimal area,
                                 BigDecimal fracaoIdeal, Long blocoId, Long proprietarioId,
                                 TipoOcupacao tipoOcupacaoAtual, boolean ocupada) {
}