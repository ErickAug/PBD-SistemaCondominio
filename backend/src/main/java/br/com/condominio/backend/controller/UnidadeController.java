package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UnidadeRequestDTO;
import br.com.condominio.backend.dto.UnidadeResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/administradoras/{administradoraId}/condominios/{condominioId}/blocos/{blocoId}/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;
    private TenantAccessGuard tenantAccessGuard;
    public UnidadeController(UnidadeService unidadeService, TenantAccessGuard tenantAccessGuard) {
        this.unidadeService = unidadeService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> cadastrar(@PathVariable Long administradoraId,
                                                        @PathVariable Long blocoId,
                                                        @RequestBody UnidadeRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        Unidade unidade = new Unidade();
        unidade.setNumero(dto.numero());
        unidade.setAndar(dto.andar());
        unidade.setArea(dto.area());
        unidade.setFracaoIdeal(dto.fracaoIdeal());

        Unidade salva = unidadeService.cadastrar(unidade, blocoId, administradoraIdAutenticado);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva, false));
    }

    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listar(@PathVariable Long administradoraId,
                                                           @PathVariable Long blocoId,
                                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        List<Unidade> unidades = unidadeService.listarPorBloco(blocoId, administradoraIdAutenticado);

        List<Long> ids = unidades.stream().map(Unidade::getId).toList();
        Set<Long> ocupadas = unidadeService.identificarUnidadesOcupadas(ids);

        List<UnidadeResponseDTO> resposta = unidades.stream()
                .map(unidade -> toResponseDTO(unidade, ocupadas.contains(unidade.getId())))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @DeleteMapping("/{unidadeId}")
    public ResponseEntity<Void> excluir(@PathVariable Long administradoraId,
                                        @PathVariable Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        unidadeService.excluir(unidadeId, administradoraIdAutenticado);

        return ResponseEntity.noContent().build();
    }

    private UnidadeResponseDTO toResponseDTO(Unidade unidade, boolean ocupada) {
        return new UnidadeResponseDTO(
                unidade.getId(),
                unidade.getNumero(),
                unidade.getAndar(),
                unidade.getArea(),
                unidade.getFracaoIdeal(),
                unidade.getBloco().getId(),
                ocupada
        );
    }
}