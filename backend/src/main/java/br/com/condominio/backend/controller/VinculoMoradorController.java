package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.*;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.VinculoMoradorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/moradores")
public class VinculoMoradorController {

    private final VinculoMoradorService vinculoMoradorService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public VinculoMoradorController(VinculoMoradorService vinculoMoradorService, UnidadeAccessGuard unidadeAccessGuard) {
        this.vinculoMoradorService = vinculoMoradorService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @PostMapping
    public ResponseEntity<VinculoMoradorResponseDTO> registrar(@PathVariable Long condominioId,
                                                               @PathVariable Long blocoId,
                                                               @PathVariable Long unidadeId,
                                                               @RequestBody VinculoMoradorRequestDTO dto,
                                                               @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        VinculoMorador salvo = vinculoMoradorService.registrar(unidade, dto.moradorId(), dto.dataEntrada());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @PatchMapping("/{vinculoId}/encerrar")
    public ResponseEntity<Void> encerrar(@PathVariable Long condominioId,
                                         @PathVariable Long blocoId,
                                         @PathVariable Long unidadeId,
                                         @PathVariable Long vinculoId,
                                         @RequestBody EncerrarVinculoRequestDTO dto,
                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        vinculoMoradorService.encerrar(vinculoId, unidadeId, dto.dataSaida());

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @GetMapping
    public ResponseEntity<List<VinculoMoradorResponseDTO>> listarAtivos(@PathVariable Long condominioId,
                                                                        @PathVariable Long blocoId,
                                                                        @PathVariable Long unidadeId,
                                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<VinculoMoradorResponseDTO> resposta = vinculoMoradorService.listarAtivos(unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private VinculoMoradorResponseDTO toResponseDTO(VinculoMorador v) {
        return new VinculoMoradorResponseDTO(v.getId(), v.getUnidade().getId(), v.getMorador().getId(),
                v.getMorador().getNome(), v.getDataEntrada(), v.getDataSaida());
    }
}