package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UnidadeRequestDTO;
import br.com.condominio.backend.dto.UnidadeResponseDTO;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;
    private final BlocoService blocoService;
    private final TenantAccessGuard tenantAccessGuard;

    public UnidadeController(UnidadeService unidadeService, BlocoService blocoService,
                             TenantAccessGuard tenantAccessGuard) {
        this.unidadeService = unidadeService;
        this.blocoService = blocoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                        @PathVariable Long blocoId,
                                                        @RequestBody UnidadeRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        Bloco bloco = blocoService.buscarValidandoCondominio(blocoId, condominioId);

        Unidade unidade = new Unidade();
        unidade.setNumero(dto.numero());
        unidade.setAndar(dto.andar());
        unidade.setArea(dto.area());
        unidade.setFracaoIdeal(dto.fracaoIdeal());

        Unidade salva = unidadeService.cadastrar(unidade, bloco);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva, false));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listar(@PathVariable Long condominioId,
                                                           @PathVariable Long blocoId,
                                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        blocoService.buscarValidandoCondominio(blocoId, condominioId);

        List<Unidade> unidades = unidadeService.listarPorBloco(blocoId);
        List<Long> ids = unidades.stream().map(Unidade::getId).toList();
        Set<Long> ocupadas = unidadeService.identificarUnidadesOcupadas(ids);

        List<UnidadeResponseDTO> resposta = unidades.stream()
                .map(unidade -> toResponseDTO(unidade, ocupadas.contains(unidade.getId())))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @DeleteMapping("/{unidadeId}")
    public ResponseEntity<Void> excluir(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        blocoService.buscarValidandoCondominio(blocoId, condominioId);
        Unidade unidade = unidadeService.buscarValidandoBloco(unidadeId, blocoId);

        unidadeService.excluir(unidade);

        return ResponseEntity.noContent().build();
    }

    private UnidadeResponseDTO toResponseDTO(Unidade unidade, boolean ocupada) {
        return new UnidadeResponseDTO(
                unidade.getId(), unidade.getNumero(), unidade.getAndar(), unidade.getArea(),
                unidade.getFracaoIdeal(), unidade.getBloco().getId(), ocupada
        );
    }
}