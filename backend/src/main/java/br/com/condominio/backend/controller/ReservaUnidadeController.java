package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.ReservaRequestDTO;
import br.com.condominio.backend.dto.ReservaResponseDTO;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/reservas")
public class ReservaUnidadeController {

    private final ReservaService reservaService;
    private final ReservaMapper mapper;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public ReservaUnidadeController(ReservaService reservaService, ReservaMapper mapper,
                                    UnidadeAccessGuard unidadeAccessGuard) {
        this.reservaService = reservaService;
        this.mapper = mapper;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @PostMapping
    public ResponseEntity<ReservaResponseDTO> reservar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @RequestBody ReservaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Reserva reserva = reservaService.reservar(unidade, usuarioAutenticado.getUsuario(), dto.areaComumId(),
                dto.data(), dto.horaInicio(), dto.horaFim(), dto.finalidade());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toReservaDTO(reserva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<ReservaResponseDTO>> listar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        return ResponseEntity.ok(reservaService.listarDaUnidade(unidadeId).stream()
                .map(mapper::toReservaDTO).toList());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @PatchMapping("/{reservaId}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @PathVariable Long reservaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Reserva cancelada = reservaService.cancelar(unidadeId, reservaId, usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(mapper.toReservaDTO(cancelada));
    }
}
