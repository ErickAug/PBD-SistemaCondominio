package br.com.condominio.backend.dto;

import java.time.LocalTime;
import java.util.List;

public record CalendarioAreaDTO(Long areaComumId, String areaComumNome, LocalTime horarioAbertura,
                                LocalTime horarioFechamento, boolean exigeAprovacao,
                                List<ReservaResponseDTO> reservas) {
}
