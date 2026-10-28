package br.com.condominio.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaDespesaResponseDTO(Long id, Integer numeroParcela, BigDecimal valor, LocalDate competencia) {
}