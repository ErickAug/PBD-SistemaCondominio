package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record AutorizadoPermanenteRequestDTO(String nome, String documento, String tipo,
                                             LocalDate dataInicio, LocalDate dataFim) {}
