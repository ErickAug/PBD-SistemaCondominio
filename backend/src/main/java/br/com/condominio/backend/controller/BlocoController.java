package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.BlocoRequestDTO;
import br.com.condominio.backend.dto.BlocoResponseDTO;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos")
public class BlocoController {

    private final BlocoService blocoService;
    private final TenantAccessGuard tenantAccessGuard;

    public BlocoController(BlocoService blocoService, TenantAccessGuard tenantAccessGuard) {
        this.blocoService = blocoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<BlocoResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                      @RequestBody BlocoRequestDTO dto,
                                                      @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Bloco bloco = new Bloco();
        bloco.setNome(dto.nome());

        Bloco salvo = blocoService.cadastrar(bloco, condominio);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<BlocoResponseDTO>> listar(@PathVariable Long condominioId,
                                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<BlocoResponseDTO> lista = blocoService.listarPorCondominio(condominioId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(lista);
    }

    private BlocoResponseDTO toResponseDTO(Bloco bloco) {
        return new BlocoResponseDTO(bloco.getId(), bloco.getNome(), bloco.getCondominio().getId());
    }
}