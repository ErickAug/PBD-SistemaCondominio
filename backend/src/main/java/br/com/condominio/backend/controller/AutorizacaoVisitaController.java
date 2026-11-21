package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AutorizacaoVisitaRequestDTO;
import br.com.condominio.backend.dto.AutorizacaoVisitaResponseDTO;
import br.com.condominio.backend.model.AutorizacaoVisita;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AutorizacaoVisitaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/visitas")
public class AutorizacaoVisitaController {

    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public AutorizacaoVisitaController(AutorizacaoVisitaService autorizacaoVisitaService,
                                        UnidadeAccessGuard unidadeAccessGuard) {
        this.autorizacaoVisitaService = autorizacaoVisitaService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @PostMapping
    public ResponseEntity<AutorizacaoVisitaResponseDTO> cadastrar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @RequestBody AutorizacaoVisitaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        AutorizacaoVisita salva = autorizacaoVisitaService.cadastrar(
                unidade, dto.nome(), dto.documento(), dto.tipo(), dto.dataInicio(), dto.dataFim());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<AutorizacaoVisitaResponseDTO>> listar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<AutorizacaoVisitaResponseDTO> resposta = autorizacaoVisitaService.listarPorUnidade(unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AutorizacaoVisitaResponseDTO> cancelar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        AutorizacaoVisita cancelada = autorizacaoVisitaService.cancelar(id, unidadeId);

        return ResponseEntity.ok(toResponseDTO(cancelada));
    }

    private AutorizacaoVisitaResponseDTO toResponseDTO(AutorizacaoVisita a) {
        return new AutorizacaoVisitaResponseDTO(a.getId(), a.getUnidade().getId(), a.getNome(), a.getDocumento(),
                a.getTipo(), a.getDataInicio(), a.getDataFim(), a.getStatus(), a.getDataUso(),
                autorizacaoVisitaService.calcularSituacao(a));
    }
}
