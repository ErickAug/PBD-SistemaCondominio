package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.FechamentoMensalRequestDTO;
import br.com.condominio.backend.dto.FechamentoMensalResponseDTO;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.FechamentoMensal;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.FechamentoMensalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/fechamentos")
public class FechamentoMensalController {

    private final FechamentoMensalService fechamentoMensalService;
    private final TenantAccessGuard tenantAccessGuard;

    public FechamentoMensalController(FechamentoMensalService fechamentoMensalService,
                                      TenantAccessGuard tenantAccessGuard) {
        this.fechamentoMensalService = fechamentoMensalService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<FechamentoMensalResponseDTO> fechar(
            @PathVariable Long condominioId,
            @RequestBody FechamentoMensalRequestDTO dto,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        FechamentoMensal salvo = fechamentoMensalService.fechar(
                condominio, dto.competencia(), usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<FechamentoMensalResponseDTO>> listar(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<FechamentoMensalResponseDTO> lista = fechamentoMensalService.listar(condominioId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(lista);
    }

    private FechamentoMensalResponseDTO toResponseDTO(FechamentoMensal f) {
        return new FechamentoMensalResponseDTO(f.getId(), f.getCompetencia(), f.getDataFechamento(),
                f.getFechadoPor().getNome());
    }
}