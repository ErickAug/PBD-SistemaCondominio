package br.com.condominio.backend.dto;

import java.util.List;

public record AvisoLeiturasResponseDTO(Long avisoId, String titulo, boolean vencido, int totalDestinatarios,
                                       int totalLeram, int totalNaoLeram,
                                       List<LeituraDestinatarioDTO> leram,
                                       List<LeituraDestinatarioDTO> naoLeram) {
}
