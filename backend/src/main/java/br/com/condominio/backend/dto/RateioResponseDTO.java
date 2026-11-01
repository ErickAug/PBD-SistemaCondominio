package br.com.condominio.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RateioResponseDTO(Long id, LocalDate competencia,
                                BigDecimal valorTotalOrdinario, BigDecimal valorTotalExtraordinario,
                                List<RateioUnidadeResponseDTO> unidades) {}