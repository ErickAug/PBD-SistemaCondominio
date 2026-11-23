package br.com.condominio.backend.dto;

public record RegistroAcessoManualRequestDTO(Long unidadeId, String nome, String documento, String tipo,
                                              String quemAutorizou, String placa) {
}
