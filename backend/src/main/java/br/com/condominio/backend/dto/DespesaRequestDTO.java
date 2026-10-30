package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoDespesa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaRequestDTO(LocalDate data, String categoria, String fornecedor, BigDecimal valor,
                                TipoDespesa tipo, String comprovante, Integer numeroParcelas) {
}