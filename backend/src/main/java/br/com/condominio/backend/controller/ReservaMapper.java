package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AreaComumResponseDTO;
import br.com.condominio.backend.dto.ReservaResponseDTO;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Reserva;
import org.springframework.stereotype.Component;

@Component
public class ReservaMapper {

    public AreaComumResponseDTO toAreaDTO(AreaComum a) {
        return new AreaComumResponseDTO(a.getId(), a.getCondominio().getId(), a.getNome(), a.getCapacidade(),
                a.getHorarioAbertura(), a.getHorarioFechamento(), a.isExigeAprovacao());
    }

    public ReservaResponseDTO toReservaDTO(Reserva r) {
        return new ReservaResponseDTO(
                r.getId(),
                r.getAreaComum().getId(),
                r.getAreaComum().getNome(),
                r.getUnidade().getId(),
                r.getUnidade().getBloco().getNome(),
                r.getUnidade().getNumero(),
                r.getData(),
                r.getHoraInicio(),
                r.getHoraFim(),
                r.getFinalidade(),
                r.getSituacao(),
                r.getCriadoPor().getNome(),
                r.getDataCriacao(),
                r.getDecididoPor() != null ? r.getDecididoPor().getNome() : null,
                r.getDataDecisao(),
                r.getMotivoRecusa(),
                r.getCanceladoPor() != null ? r.getCanceladoPor().getNome() : null,
                r.getDataCancelamento());
    }
}
