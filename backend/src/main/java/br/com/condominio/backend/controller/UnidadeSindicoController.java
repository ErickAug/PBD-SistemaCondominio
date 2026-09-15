package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UnidadeRequestDTO;
import br.com.condominio.backend.dto.UnidadeResponseDTO;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades")
public class UnidadeSindicoController {

    private final UnidadeService unidadeService;
    private final TenantAccessGuard tenantAccessGuard;

    public UnidadeSindicoController(UnidadeService unidadeService, TenantAccessGuard tenantAccessGuard) {
        this.unidadeService = unidadeService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> cadastrar(@PathVariable Long condominioId,
            @PathVariable Long blocoId,
            @RequestBody UnidadeRequestDTO dto,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long condominioIdAutenticado = tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        Unidade unidade = new Unidade();
        unidade.setNumero(dto.numero());
        unidade.setAndar(dto.andar());
        unidade.setArea(dto.area());
        unidade.setFracaoIdeal(dto.fracaoIdeal());

        Unidade salva = unidadeService.cadastrarParaSindico(unidade, blocoId, condominioIdAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva, false));
    }

    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listar(@PathVariable Long condominioId,
            @PathVariable Long blocoId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long condominioIdAutenticado = tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        List<Unidade> unidades = unidadeService.listarPorBlocoParaSindico(blocoId, condominioIdAutenticado);

        List<Long> ids = unidades.stream().map(Unidade::getId).toList();
        Set<Long> ocupadas = unidadeService.identificarUnidadesOcupadas(ids);

        List<UnidadeResponseDTO> resposta = unidades.stream()
                .map(u -> toResponseDTO(u, ocupadas.contains(u.getId())))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @DeleteMapping("/{unidadeId}")
    public ResponseEntity<Void> excluir(@PathVariable Long condominioId,
            @PathVariable Long unidadeId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long condominioIdAutenticado = tenantAccessGuard.validarSindicoDoCondominio(condominioId, usuarioAutenticado);

        unidadeService.excluirParaSindico(unidadeId, condominioIdAutenticado);
        return ResponseEntity.noContent().build();
    }

    private UnidadeResponseDTO toResponseDTO(Unidade unidade, boolean ocupada) {
        return new UnidadeResponseDTO(
                unidade.getId(), unidade.getNumero(), unidade.getAndar(),
                unidade.getArea(), unidade.getFracaoIdeal(),
                unidade.getBloco().getId(), ocupada);
    }
}