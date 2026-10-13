package br.com.condominio.backend.dto;

public record VeiculoResponseDTO(Long id, String placa, String modelo, String cor,
                                 Long donoId, String donoNome, Long unidadeId,
                                 Long vagaId, String vagaNumero, boolean ativo) {}
