package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.DependenteRequestDTO;
import br.com.condominio.backend.dto.DependenteResponseDTO;
import br.com.condominio.backend.model.Dependente;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.DependenteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/dependentes")
public class DependenteController {

    private final DependenteService dependenteService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public DependenteController(DependenteService dependenteService, UnidadeAccessGuard unidadeAccessGuard) {
        this.dependenteService = dependenteService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @PostMapping
    public ResponseEntity<DependenteResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                           @PathVariable Long blocoId,
                                                           @PathVariable Long unidadeId,
                                                           @RequestBody DependenteRequestDTO dto,
                                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Dependente salvo = dependenteService.cadastrar(unidade, dto.nome());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @PatchMapping("/{dependenteId}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long condominioId,
                                          @PathVariable Long blocoId,
                                          @PathVariable Long unidadeId,
                                          @PathVariable Long dependenteId,
                                          @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        dependenteService.desativar(dependenteId, unidadeId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @GetMapping
    public ResponseEntity<List<DependenteResponseDTO>> listarAtivos(@PathVariable Long condominioId,
                                                                    @PathVariable Long blocoId,
                                                                    @PathVariable Long unidadeId,
                                                                    @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<DependenteResponseDTO> resposta = dependenteService.listarAtivos(unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private DependenteResponseDTO toResponseDTO(Dependente d) {
        return new DependenteResponseDTO(d.getId(), d.getUnidade().getId(), d.getNome(), d.isAtivo());
    }
}