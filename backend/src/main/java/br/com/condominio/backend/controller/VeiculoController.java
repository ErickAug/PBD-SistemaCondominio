package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.VeiculoRequestDTO;
import br.com.condominio.backend.dto.VeiculoResponseDTO;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.VeiculoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;
    private final UnidadeAccessGuard unidadeAccessGuard;

    public VeiculoController(VeiculoService veiculoService, UnidadeAccessGuard unidadeAccessGuard) {
        this.veiculoService = veiculoService;
        this.unidadeAccessGuard = unidadeAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                        @PathVariable Long blocoId,
                                                        @PathVariable Long unidadeId,
                                                        @RequestBody VeiculoRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Veiculo salvo = veiculoService.cadastrar(unidade, dto.donoId(), dto.placa(),
                dto.modelo(), dto.cor(), dto.vagaId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @PatchMapping("/{veiculoId}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long condominioId,
                                          @PathVariable Long blocoId,
                                          @PathVariable Long unidadeId,
                                          @PathVariable Long veiculoId,
                                          @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        veiculoService.desativar(veiculoId, unidadeId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listar(@PathVariable Long condominioId,
                                                           @PathVariable Long blocoId,
                                                           @PathVariable Long unidadeId,
                                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<VeiculoResponseDTO> resposta = veiculoService.listarAtivosDaUnidade(unidadeId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private VeiculoResponseDTO toResponseDTO(Veiculo v) {
        return new VeiculoResponseDTO(
                v.getId(), v.getPlaca(), v.getModelo(), v.getCor(),
                v.getDono().getId(), v.getDono().getNome(), v.getUnidade().getId(),
                v.getVaga() != null ? v.getVaga().getId() : null,
                v.getVaga() != null ? v.getVaga().getNumero() : null,
                v.isAtivo()
        );
    }
}