package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.BlocoRequestDTO;
import br.com.condominio.backend.dto.BlocoResponseDTO;
import br.com.condominio.backend.dto.StatusFracaoIdealResponseDTO;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos")
public class BlocoSindicoController {

    private final BlocoService blocoService;
    private final UnidadeService unidadeService;
    private final TenantAccessGuard tenantAccessGuard;

    public BlocoSindicoController(BlocoService blocoService,
            UnidadeService unidadeService,
            TenantAccessGuard tenantAccessGuard) {
        this.blocoService = blocoService;
        this.unidadeService = unidadeService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<BlocoResponseDTO> cadastrar(@PathVariable Long condominioId,
            @RequestBody BlocoRequestDTO dto,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        Bloco bloco = new Bloco();
        bloco.setNome(dto.nome());

        Bloco salvo = blocoService.cadastrarParaSindico(bloco, condominioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @GetMapping
    public ResponseEntity<List<BlocoResponseDTO>> listar(@PathVariable Long condominioId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        List<BlocoResponseDTO> lista = blocoService.listarPorCondominioParaSindico(condominioId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/fracao-ideal")
    public ResponseEntity<StatusFracaoIdealResponseDTO> statusFracaoIdeal(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        UnidadeService.StatusFracaoIdeal status = unidadeService.calcularStatusFracaoParaSindico(condominioId);

        return ResponseEntity.ok(new StatusFracaoIdealResponseDTO(
                status.somaAtual(), status.diferencaParaFechar(), status.fechaEm100()));
    }

    private BlocoResponseDTO toResponseDTO(Bloco bloco) {
        return new BlocoResponseDTO(bloco.getId(), bloco.getNome(), bloco.getCondominio().getId());
    }
}