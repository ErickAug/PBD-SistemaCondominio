package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.BlocoRequestDTO;
import br.com.condominio.backend.dto.BlocoResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/administradoras/{administradoraId}/condominios/{condominioId}/blocos")
public class BlocoController {

    private final BlocoService blocoService;
    private TenantAccessGuard tenantAccessGuard;

    public BlocoController(BlocoService blocoService, TenantAccessGuard tenantAccessGuard) {
        this.blocoService = blocoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<BlocoResponseDTO> cadastrar(@PathVariable Long administradoraId,
                                                      @PathVariable Long condominioId,
                                                      @RequestBody BlocoRequestDTO dto,
                                                      @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        Bloco bloco = new Bloco();
        bloco.setNome(dto.nome());

        Bloco salvo = blocoService.cadastrar(bloco, condominioId, administradoraIdAutenticado);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @GetMapping
    public ResponseEntity<List<BlocoResponseDTO>> listar(@PathVariable Long administradoraId,
                                                         @PathVariable Long condominioId,
                                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        List<BlocoResponseDTO> lista = blocoService.listarPorCondominio(condominioId, administradoraIdAutenticado)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(lista);
    }


    private BlocoResponseDTO toResponseDTO(Bloco bloco) {
        return new BlocoResponseDTO(bloco.getId(), bloco.getNome(), bloco.getCondominio().getId());
    }
}