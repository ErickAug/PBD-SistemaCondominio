package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record FechamentoMensalResponseDTO(Long id, LocalDate competencia, LocalDate dataFechamento, String fechadoPorNome) {}
