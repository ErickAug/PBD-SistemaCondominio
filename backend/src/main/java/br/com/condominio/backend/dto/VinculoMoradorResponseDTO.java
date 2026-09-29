package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record VinculoMoradorResponseDTO(Long id, Long unidadeId, Long moradorId, String moradorNome,
                                        LocalDate dataEntrada, LocalDate dataSaida) {}
