package br.com.condominio.backend.dto;

public record VeiculoBuscaResponseDTO(Long veiculoId, String placa, String modelo, String cor,
                                      String bloco, String unidadeNumero, Long unidadeId,
                                      String donoNome, String donoContato, String vagaNumero) {}