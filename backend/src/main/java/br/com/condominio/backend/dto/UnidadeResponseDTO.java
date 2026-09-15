package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record UnidadeResponseDTO(Long id, String numero, Integer andar, BigDecimal area,
                                 BigDecimal fracaoIdeal, Long blocoId, boolean ocupada) {
}