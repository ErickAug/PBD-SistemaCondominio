package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.StatusFracaoIdealResponseDTO;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/condominios/{condominioId}")
public class CondominioGestaoController {

    private final UnidadeService unidadeService;
    private final TenantAccessGuard tenantAccessGuard;

    public CondominioGestaoController(UnidadeService unidadeService, TenantAccessGuard tenantAccessGuard) {
        this.unidadeService = unidadeService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/fracao-ideal")
    public ResponseEntity<StatusFracaoIdealResponseDTO> statusFracaoIdeal(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        UnidadeService.StatusFracaoIdeal status = unidadeService.calcularStatusFracao(condominioId);

        return ResponseEntity.ok(new StatusFracaoIdealResponseDTO(
                status.somaAtual(), status.diferencaParaFechar(), status.fechaEm100()));
    }
}