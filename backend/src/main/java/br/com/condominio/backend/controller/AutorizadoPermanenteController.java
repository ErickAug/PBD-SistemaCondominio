package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AutorizadoPermanenteRequestDTO;
import br.com.condominio.backend.dto.AutorizadoPermanenteResponseDTO;
import br.com.condominio.backend.model.AutorizadoPermanente;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AutorizadoPermanenteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/autorizados")
public class AutorizadoPermanenteController {

    private final AutorizadoPermanenteService autorizadoPermanenteService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public AutorizadoPermanenteController(AutorizadoPermanenteService autorizadoPermanenteService,
                                          UnidadeAccessGuard unidadeAccessGuard) {
        this.autorizadoPermanenteService = autorizadoPermanenteService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @PostMapping
    public ResponseEntity<AutorizadoPermanenteResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                                     @PathVariable Long blocoId,
                                                                     @PathVariable Long unidadeId,
                                                                     @RequestBody AutorizadoPermanenteRequestDTO dto,
                                                                     @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        AutorizadoPermanente salvo = autorizadoPermanenteService.cadastrar(
                unidade, dto.nome(), dto.documento(), dto.tipo(), dto.dataInicio(), dto.dataFim());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR')")
    @GetMapping
    public ResponseEntity<List<AutorizadoPermanenteResponseDTO>> listar(@PathVariable Long condominioId,
                                                                        @PathVariable Long blocoId,
                                                                        @PathVariable Long unidadeId,
                                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<AutorizadoPermanenteResponseDTO> resposta = autorizadoPermanenteService.listarPorUnidade(unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private AutorizadoPermanenteResponseDTO toResponseDTO(AutorizadoPermanente a) {
        return new AutorizadoPermanenteResponseDTO(a.getId(), a.getUnidade().getId(), a.getNome(), a.getDocumento(),
                a.getTipo(), a.getDataInicio(), a.getDataFim(), autorizadoPermanenteService.estaLiberadoHoje(a));
    }
}