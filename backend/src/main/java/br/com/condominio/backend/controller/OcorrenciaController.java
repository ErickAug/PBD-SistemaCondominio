package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.CorrecaoOcorrenciaRequestDTO;
import br.com.condominio.backend.dto.OcorrenciaRequestDTO;
import br.com.condominio.backend.dto.OcorrenciaResponseDTO;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.OcorrenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/portaria/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final TenantAccessGuard tenantAccessGuard;

    public OcorrenciaController(OcorrenciaService ocorrenciaService, TenantAccessGuard tenantAccessGuard) {
        this.ocorrenciaService = ocorrenciaService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping
    public ResponseEntity<OcorrenciaResponseDTO> registrar(@PathVariable Long condominioId,
                                        @RequestBody OcorrenciaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        Ocorrencia ocorrencia = ocorrenciaService.registrar(condominioId, dto.texto(), dto.tipo(), dto.unidadeId(),
                usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(ocorrencia));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/{ocorrenciaId}/correcao")
    public ResponseEntity<OcorrenciaResponseDTO> corrigir(@PathVariable Long condominioId,
                                        @PathVariable Long ocorrenciaId,
                                        @RequestBody CorrecaoOcorrenciaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        Ocorrencia correcao = ocorrenciaService.corrigir(condominioId, ocorrenciaId, dto.texto(),
                usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(correcao));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/{ocorrenciaId}/resolver")
    public ResponseEntity<OcorrenciaResponseDTO> resolver(@PathVariable Long condominioId,
                                        @PathVariable Long ocorrenciaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        Ocorrencia ocorrencia = ocorrenciaService.marcarResolvida(condominioId, ocorrenciaId,
                usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(toResponseDTO(ocorrencia));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping
    public ResponseEntity<List<OcorrenciaResponseDTO>> listar(@PathVariable Long condominioId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                        @RequestParam(required = false) String tipo,
                                        @RequestParam(required = false) Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        List<OcorrenciaResponseDTO> resposta = ocorrenciaService.filtrar(condominioId, inicio, fim, tipo, unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping("/pendentes")
    public ResponseEntity<List<OcorrenciaResponseDTO>> listarPendentes(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        List<OcorrenciaResponseDTO> resposta = ocorrenciaService.listarPendentes(condominioId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private OcorrenciaResponseDTO toResponseDTO(Ocorrencia o) {
        return new OcorrenciaResponseDTO(
                o.getId(),
                o.getUnidade() != null ? o.getUnidade().getId() : null,
                o.getUnidade() != null ? o.getUnidade().getBloco().getNome() : null,
                o.getUnidade() != null ? o.getUnidade().getNumero() : null,
                o.getTexto(),
                o.getTipo(),
                o.getAutor().getNome(),
                o.getDataHora(),
                o.getCorrecaoDe() != null ? o.getCorrecaoDe().getId() : null,
                o.isResolvida(),
                o.getResolvidoPor() != null ? o.getResolvidoPor().getNome() : null,
                o.getResolvidoEm());
    }
}
