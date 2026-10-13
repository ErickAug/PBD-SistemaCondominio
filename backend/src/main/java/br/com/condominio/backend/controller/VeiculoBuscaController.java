package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.VeiculoBuscaResponseDTO;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.VeiculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/condominios/{condominioId}/veiculos")
public class VeiculoBuscaController {

    private final VeiculoService veiculoService;
    private final TenantAccessGuard tenantAccessGuard;

    public VeiculoBuscaController(VeiculoService veiculoService, TenantAccessGuard tenantAccessGuard) {
        this.veiculoService = veiculoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping("/busca")
    public ResponseEntity<VeiculoBuscaResponseDTO> buscarPorPlaca(
            @PathVariable Long condominioId,
            @RequestParam String placa,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        return veiculoService.buscarPorPlaca(condominioId, placa)
                .map(v -> ResponseEntity.ok(toBuscaDTO(v)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private VeiculoBuscaResponseDTO toBuscaDTO(Veiculo v) {
        return new VeiculoBuscaResponseDTO(
                v.getId(), v.getPlaca(), v.getModelo(), v.getCor(),
                v.getUnidade().getBloco().getNome(), v.getUnidade().getNumero(), v.getUnidade().getId(),
                v.getDono().getNome(), v.getDono().getContato(),
                v.getVaga() != null ? v.getVaga().getNumero() : null
        );
    }
}