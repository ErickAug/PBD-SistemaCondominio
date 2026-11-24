package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoReuniao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ReuniaoDetalheDTO(Long id, Long condominioId, String titulo, LocalDate data, LocalTime hora,
                                String local, SituacaoReuniao situacao, String convocadaPorNome,
                                LocalDateTime dataPublicacao, List<PautaItemDTO> pauta,
                                List<PresencaResponseDTO> presencas, int unidadesPresentes,
                                int unidadesDoCondominio, AtaResponseDTO ata) {
}
