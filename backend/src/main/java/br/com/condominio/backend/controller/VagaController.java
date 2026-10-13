package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.*;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Vaga;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.VagaService;
import br.com.condominio.backend.service.VeiculoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/vagas")
public class VagaController {

    private final VagaService vagaService;
    private final VeiculoService veiculoService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public VagaController(VagaService vagaService, VeiculoService veiculoService,
                          UnidadeAccessGuard unidadeAccessGuard) {
        this.vagaService = vagaService;
        this.veiculoService = veiculoService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<VagaResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                     @PathVariable Long blocoId,
                                                     @PathVariable Long unidadeId,
                                                     @RequestBody VagaRequestDTO dto,
                                                     @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Vaga salva = vagaService.cadastrar(unidade, dto.numero(), dto.tipo());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva, null));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @DeleteMapping("/{vagaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @PathVariable Long vagaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        vagaService.excluir(vagaId, unidadeId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<VagasDaUnidadeResponseDTO> listar(@PathVariable Long condominioId,
                                                            @PathVariable Long blocoId,
                                                            @PathVariable Long unidadeId,
                                                            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<Vaga> vagas = vagaService.listarPorUnidade(unidadeId);
        List<Long> ids = vagas.stream().map(Vaga::getId).toList();
        Map<Long, Veiculo> ocupantes = veiculoService.buscarPorVagas(ids);

        List<VagaResponseDTO> lista = vagas.stream()
                .map(vaga -> toResponseDTO(vaga, ocupantes.get(vaga.getId())))
                .toList();

        return ResponseEntity.ok(new VagasDaUnidadeResponseDTO(lista.size(), lista));
    }

    private VagaResponseDTO toResponseDTO(Vaga vaga, Veiculo ocupante) {
        VeiculoResumoDTO resumo = ocupante == null ? null
                : new VeiculoResumoDTO(ocupante.getId(), ocupante.getPlaca(), ocupante.getModelo(), ocupante.getCor());

        return new VagaResponseDTO(vaga.getId(), vaga.getNumero(), vaga.getTipo(), vaga.getUnidade().getId(), resumo);
    }
}