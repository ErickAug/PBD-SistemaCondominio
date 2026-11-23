package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.OrigemRegistroAcesso;

import java.time.LocalDateTime;

public record RegistroAcessoResponseDTO(Long id, Long unidadeId, String blocoNome, String unidadeNumero,
                                         String nome, String documento, String tipo,
                                         OrigemRegistroAcesso origem, String quemAutorizou,
                                         String placaVeiculo, String porteiroNome,
                                         LocalDateTime horaEntrada, LocalDateTime horaSaida) {
}
