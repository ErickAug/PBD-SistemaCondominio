package br.com.condominio.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DespesasListaResponseDTO(List<DespesaResponseDTO> despesas, BigDecimal total) {
}