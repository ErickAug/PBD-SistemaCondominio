package br.com.condominio.backend.dto;

public record VeiculoRequestDTO(Long donoId, String placa, String modelo, String cor, Long vagaId) {}