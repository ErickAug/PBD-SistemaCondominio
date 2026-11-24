package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AreaComumRequestDTO;
import br.com.condominio.backend.dto.AreaComumResponseDTO;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AreaComumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/areas-comuns")
public class AreaComumController {

    private final AreaComumService areaComumService;
    private final ReservaMapper mapper;
    private final TenantAccessGuard tenantAccessGuard;

    public AreaComumController(AreaComumService areaComumService, ReservaMapper mapper,
                               TenantAccessGuard tenantAccessGuard) {
        this.areaComumService = areaComumService;
        this.mapper = mapper;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<AreaComumResponseDTO> cadastrar(@PathVariable Long condominioId,
                                        @RequestBody AreaComumRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        AreaComum area = areaComumService.cadastrar(condominio, dto.nome(), dto.capacidade(),
                dto.horarioAbertura(), dto.horarioFechamento(), dto.exigeAprovacao());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAreaDTO(area));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<AreaComumResponseDTO>> listar(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        return ResponseEntity.ok(areaComumService.listar(condominioId).stream().map(mapper::toAreaDTO).toList());
    }
}
