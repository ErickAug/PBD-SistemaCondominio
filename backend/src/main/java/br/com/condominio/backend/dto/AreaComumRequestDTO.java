package br.com.condominio.backend.dto;

import java.time.LocalTime;

public record AreaComumRequestDTO(String nome, Integer capacidade, LocalTime horarioAbertura,
                                  LocalTime horarioFechamento, Boolean exigeAprovacao) {
}
