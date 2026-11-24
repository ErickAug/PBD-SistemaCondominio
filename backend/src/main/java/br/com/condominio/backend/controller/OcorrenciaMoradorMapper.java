package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.HistoricoOcorrenciaMoradorDTO;
import br.com.condominio.backend.dto.OcorrenciaMoradorDetalheDTO;
import br.com.condominio.backend.dto.OcorrenciaMoradorResumoDTO;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.OcorrenciaMoradorHistorico;
import br.com.condominio.backend.service.OcorrenciaMoradorService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcorrenciaMoradorMapper {

    private final OcorrenciaMoradorService ocorrenciaMoradorService;

    public OcorrenciaMoradorMapper(OcorrenciaMoradorService ocorrenciaMoradorService) {
        this.ocorrenciaMoradorService = ocorrenciaMoradorService;
    }

    public OcorrenciaMoradorResumoDTO toResumo(OcorrenciaMorador o) {
        return new OcorrenciaMoradorResumoDTO(
                o.getId(),
                o.getUnidade().getId(),
                o.getUnidade().getBloco().getNome(),
                o.getUnidade().getNumero(),
                o.getAssunto(),
                o.getTipo(),
                o.getSituacao(),
                o.getFoto() != null,
                o.getAbertoPor().getNome(),
                o.getDataAbertura(),
                o.getRespondidoPor() != null ? o.getRespondidoPor().getNome() : null,
                o.getUltimaResposta(),
                o.getDataUltimaResposta());
    }

    public OcorrenciaMoradorDetalheDTO toDetalhe(OcorrenciaMorador o) {
        List<HistoricoOcorrenciaMoradorDTO> historico = ocorrenciaMoradorService.listarHistorico(o.getId())
                .stream().map(this::toHistorico).toList();

        return new OcorrenciaMoradorDetalheDTO(
                o.getId(),
                o.getCondominio().getId(),
                o.getUnidade().getId(),
                o.getUnidade().getBloco().getNome(),
                o.getUnidade().getNumero(),
                o.getAssunto(),
                o.getTipo(),
                o.getDescricao(),
                o.getFoto(),
                o.getSituacao(),
                o.getAbertoPor().getId(),
                o.getAbertoPor().getNome(),
                o.getDataAbertura(),
                o.getRespondidoPor() != null ? o.getRespondidoPor().getId() : null,
                o.getRespondidoPor() != null ? o.getRespondidoPor().getNome() : null,
                o.getUltimaResposta(),
                o.getDataUltimaResposta(),
                historico);
    }

    private HistoricoOcorrenciaMoradorDTO toHistorico(OcorrenciaMoradorHistorico h) {
        return new HistoricoOcorrenciaMoradorDTO(
                h.getSituacaoAnterior(),
                h.getSituacaoNova(),
                h.getResposta(),
                h.getResponsavel().getId(),
                h.getResponsavel().getNome(),
                h.getDataHora());
    }
}
