package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.CalendarioAreaDTO;
import br.com.condominio.backend.dto.RecusaReservaRequestDTO;
import br.com.condominio.backend.dto.ReservaResponseDTO;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/reservas")
public class ReservaGestaoController {

    private final ReservaService reservaService;
    private final ReservaMapper mapper;
    private final TenantAccessGuard tenantAccessGuard;

    public ReservaGestaoController(ReservaService reservaService, ReservaMapper mapper,
                                   TenantAccessGuard tenantAccessGuard) {
        this.reservaService = reservaService;
        this.mapper = mapper;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/pendentes")
    public ResponseEntity<List<ReservaResponseDTO>> listarPendentes(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        return ResponseEntity.ok(reservaService.listarPendentes(condominioId).stream()
                .map(mapper::toReservaDTO).toList());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PatchMapping("/{reservaId}/aprovar")
    public ResponseEntity<ReservaResponseDTO> aprovar(@PathVariable Long condominioId,
                                        @PathVariable Long reservaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Reserva reserva = reservaService.aprovar(condominioId, reservaId, usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(mapper.toReservaDTO(reserva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PatchMapping("/{reservaId}/recusar")
    public ResponseEntity<ReservaResponseDTO> recusar(@PathVariable Long condominioId,
                                        @PathVariable Long reservaId,
                                        @RequestBody RecusaReservaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Reserva reserva = reservaService.recusar(condominioId, reservaId, usuarioAutenticado.getUsuario(),
                dto.motivo());

        return ResponseEntity.ok(mapper.toReservaDTO(reserva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioAreaDTO>> calendario(@PathVariable Long condominioId,
                                        @RequestParam(required = false) String mes,
                                        @RequestParam(required = false) Long areaComumId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        List<CalendarioAreaDTO> resposta = reservaService.montarCalendario(condominioId, mes, areaComumId)
                .entrySet().stream()
                .map(e -> new CalendarioAreaDTO(
                        e.getKey().getId(),
                        e.getKey().getNome(),
                        e.getKey().getHorarioAbertura(),
                        e.getKey().getHorarioFechamento(),
                        e.getKey().isExigeAprovacao(),
                        e.getValue().stream().map(mapper::toReservaDTO).toList()))
                .toList();

        return ResponseEntity.ok(resposta);
    }
}
