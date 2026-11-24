package br.com.condominio.backend.dto;

import java.time.LocalTime;

public record AreaComumResponseDTO(Long id, Long condominioId, String nome, Integer capacidade,
                                   LocalTime horarioAbertura, LocalTime horarioFechamento,
                                   boolean exigeAprovacao) {
}
