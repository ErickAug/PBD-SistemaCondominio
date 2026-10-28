package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoDespesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DespesaResponseDTO(Long id, LocalDate data, String categoria, String fornecedor, BigDecimal valor,
                                 TipoDespesa tipo, String comprovante, Long condominioId,
                                 List<ParcelaDespesaResponseDTO> parcelas) {
}