package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.OcorrenciaMoradorDetalheDTO;
import br.com.condominio.backend.dto.OcorrenciaMoradorResumoDTO;
import br.com.condominio.backend.dto.RespostaOcorrenciaMoradorRequestDTO;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.OcorrenciaMoradorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/ocorrencias")
public class OcorrenciaMoradorGestaoController {

    private final OcorrenciaMoradorService ocorrenciaMoradorService;
    private final OcorrenciaMoradorMapper mapper;
    private final TenantAccessGuard tenantAccessGuard;

    public OcorrenciaMoradorGestaoController(OcorrenciaMoradorService ocorrenciaMoradorService,
                                             OcorrenciaMoradorMapper mapper,
                                             TenantAccessGuard tenantAccessGuard) {
        this.ocorrenciaMoradorService = ocorrenciaMoradorService;
        this.mapper = mapper;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<OcorrenciaMoradorResumoDTO>> listarFila(@PathVariable Long condominioId,
                                        @RequestParam(required = false) SituacaoOcorrenciaMorador situacao,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<OcorrenciaMoradorResumoDTO> resposta = ocorrenciaMoradorService.listarFila(condominioId, situacao)
                .stream().map(mapper::toResumo).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/{ocorrenciaId}")
    public ResponseEntity<OcorrenciaMoradorDetalheDTO> detalhar(@PathVariable Long condominioId,
                                        @PathVariable Long ocorrenciaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        OcorrenciaMorador ocorrencia = ocorrenciaMoradorService.detalharNoCondominio(condominioId, ocorrenciaId);

        return ResponseEntity.ok(mapper.toDetalhe(ocorrencia));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping("/{ocorrenciaId}/resposta")
    public ResponseEntity<OcorrenciaMoradorDetalheDTO> responder(@PathVariable Long condominioId,
                                        @PathVariable Long ocorrenciaId,
                                        @RequestBody RespostaOcorrenciaMoradorRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        OcorrenciaMorador ocorrencia = ocorrenciaMoradorService.responder(condominioId, ocorrenciaId,
                usuarioAutenticado.getUsuario(), dto.situacao(), dto.resposta());

        return ResponseEntity.ok(mapper.toDetalhe(ocorrencia));
    }
}
