package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.OcorrenciaResponseDTO;
import br.com.condominio.backend.dto.PassagemTurnoResponseDTO;
import br.com.condominio.backend.model.Ocorrencia;
import br.com.condominio.backend.model.PassagemTurno;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.PassagemTurnoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/condominios/{condominioId}/portaria/turnos")
public class PassagemTurnoController {

    private final PassagemTurnoService passagemTurnoService;
    private final TenantAccessGuard tenantAccessGuard;

    public PassagemTurnoController(PassagemTurnoService passagemTurnoService, TenantAccessGuard tenantAccessGuard) {
        this.passagemTurnoService = passagemTurnoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/encerrar")
    public ResponseEntity<PassagemTurnoResponseDTO> encerrar(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        PassagemTurno passagemTurno = passagemTurnoService.encerrarTurno(condominioId,
                usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(passagemTurno));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping("/ultima-pendencia")
    public ResponseEntity<PassagemTurnoResponseDTO> ultimaPendencia(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        Optional<PassagemTurno> passagemTurno = passagemTurnoService.buscarUltimoTurno(condominioId);

        return passagemTurno.map(p -> ResponseEntity.ok(toResponseDTO(p)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private PassagemTurnoResponseDTO toResponseDTO(PassagemTurno p) {
        List<OcorrenciaResponseDTO> pendencias = p.getPendencias().stream()
                .map(this::toOcorrenciaResponseDTO).toList();

        return new PassagemTurnoResponseDTO(p.getId(), p.getPorteiro().getNome(), p.getHorarioEncerramento(),
                pendencias);
    }

    private OcorrenciaResponseDTO toOcorrenciaResponseDTO(Ocorrencia o) {
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
