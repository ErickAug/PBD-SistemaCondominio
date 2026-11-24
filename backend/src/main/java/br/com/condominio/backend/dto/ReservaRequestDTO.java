package br.com.condominio.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequestDTO(Long areaComumId, LocalDate data, LocalTime horaInicio, LocalTime horaFim,
                                String finalidade) {
}
