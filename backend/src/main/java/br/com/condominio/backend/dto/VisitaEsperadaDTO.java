package br.com.condominio.backend.dto;

import java.time.LocalDate;

public record VisitaEsperadaDTO(String origem, Long id, Long unidadeId, String blocoNome, String unidadeNumero,
                                 String nome, String documento, String tipo,
                                 LocalDate dataInicio, LocalDate dataFim, String situacao) {
}
