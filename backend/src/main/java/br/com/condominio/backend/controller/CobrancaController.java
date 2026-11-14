package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.BaixaRequestDTO;
import br.com.condominio.backend.dto.CobrancaResponseDTO;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.CobrancaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/cobrancas")
public class CobrancaController {

    private final CobrancaService cobrancaService;
    private final TenantAccessGuard tenantAccessGuard;

    public CobrancaController(CobrancaService cobrancaService, TenantAccessGuard tenantAccessGuard) {
        this.cobrancaService = cobrancaService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<CobrancaResponseDTO>> listar(
            @PathVariable Long condominioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
            @RequestParam(required = false) SituacaoCobranca situacao,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<CobrancaResponseDTO> lista = cobrancaService.listar(condominioId, competencia, situacao)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(lista);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/inadimplencia")
    public ResponseEntity<List<CobrancaResponseDTO>> inadimplencia(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<CobrancaResponseDTO> lista = cobrancaService.listarInadimplencia(condominioId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(lista);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping("/{cobrancaId}/baixa")
    public ResponseEntity<CobrancaResponseDTO> darBaixa(
            @PathVariable Long condominioId,
            @PathVariable Long cobrancaId,
            @RequestBody BaixaRequestDTO dto,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Cobranca cobranca = cobrancaService.darBaixa(cobrancaId, condominioId, dto.valorPago(),
                dto.dataPagamento(), dto.formaPagamento(), usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(toResponseDTO(cobranca));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @DeleteMapping("/{cobrancaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long condominioId,
            @PathVariable Long cobrancaId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        cobrancaService.excluir(cobrancaId, condominioId);

        return ResponseEntity.noContent().build();
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