package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.CobrancaResponseDTO;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.CobrancaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/cobrancas")
public class CobrancaUnidadeController {

    private final CobrancaService cobrancaService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public CobrancaUnidadeController(CobrancaService cobrancaService, UnidadeAccessGuard unidadeAccessGuard) {
        this.cobrancaService = cobrancaService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<CobrancaResponseDTO>> listar(
            @PathVariable Long condominioId, @PathVariable Long blocoId, @PathVariable Long unidadeId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<CobrancaResponseDTO> lista = cobrancaService.listarPorUnidade(unidade.getId())
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(lista);
    }

    private CobrancaResponseDTO toResponseDTO(Cobranca cobranca) {
        var rateioUnidade = cobranca.getRateioUnidade();
        var unidade = rateioUnidade.getUnidade();
        boolean emAtraso = cobranca.isEmAtraso();
        Long diasAtraso = emAtraso ? ChronoUnit.DAYS.between(cobranca.getVencimento(), LocalDate.now()) : null;

        return new CobrancaResponseDTO(
                cobranca.getId(), unidade.getId(), unidade.getNumero(),
                rateioUnidade.getRateioMensal().getCompetencia(), cobranca.getVencimento(),
                cobranca.getSituacao(), emAtraso, diasAtraso,
                rateioUnidade.getValorOrdinario(), rateioUnidade.getValorExtraordinario(), rateioUnidade.getValorTotal(),
                rateioUnidade.getResponsavelOrdinario() != null ? rateioUnidade.getResponsavelOrdinario().getNome() : null,
                rateioUnidade.getResponsavelExtraordinario() != null ? rateioUnidade.getResponsavelExtraordinario().getNome() : null,
                cobranca.getValorPago(), cobranca.getDataPagamento(), cobranca.getFormaPagamento(),
                cobranca.getDiferenca(),
                cobranca.getBaixadoPor() != null ? cobranca.getBaixadoPor().getNome() : null
        );
    }
}