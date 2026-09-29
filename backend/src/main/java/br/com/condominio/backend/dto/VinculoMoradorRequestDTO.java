package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record VinculoMoradorRequestDTO(Long moradorId, LocalDate dataEntrada) {}