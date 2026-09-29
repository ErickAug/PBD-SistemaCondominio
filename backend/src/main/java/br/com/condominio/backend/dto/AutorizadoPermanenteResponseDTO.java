package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record AutorizadoPermanenteResponseDTO(Long id, Long unidadeId, String nome, String documento,
                                              String tipo, LocalDate dataInicio, LocalDate dataFim,
                                              boolean liberadoHoje) {}