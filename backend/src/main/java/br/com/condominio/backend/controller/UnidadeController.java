package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.ProprietarioRequestDTO;
import br.com.condominio.backend.dto.UnidadeRequestDTO;
import br.com.condominio.backend.dto.UnidadeResponseDTO;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.OcupacaoService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;
    private final BlocoService blocoService;
    private final TenantAccessGuard tenantAccessGuard;
    private final OcupacaoService ocupacaoService;

    public UnidadeController(UnidadeService unidadeService, BlocoService blocoService,
                             TenantAccessGuard tenantAccessGuard, OcupacaoService ocupacaoService) {
        this.unidadeService = unidadeService;
        this.blocoService = blocoService;
        this.tenantAccessGuard = tenantAccessGuard;
        this.ocupacaoService = ocupacaoService;
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

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva, null));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PatchMapping("/{unidadeId}/proprietario")
    public ResponseEntity<UnidadeResponseDTO> definirProprietario(
            @PathVariable Long condominioId, @PathVariable Long blocoId, @PathVariable Long unidadeId,
            @RequestBody ProprietarioRequestDTO dto,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        blocoService.buscarValidandoCondominio(blocoId, condominioId);
        Unidade unidade = unidadeService.buscarValidandoBloco(unidadeId, blocoId);

        Unidade atualizada = unidadeService.definirProprietario(unidade, dto.proprietarioId(), condominioId);

        return ResponseEntity.ok(toResponseDTO(atualizada, null));
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
        Map<Long, Ocupacao> ocupacoesAtuais = ocupacaoService.buscarOcupacoesAtuaisEmLote(ids);

        List<UnidadeResponseDTO> resposta = unidades.stream()
                .map(unidade -> toResponseDTO(unidade, ocupacoesAtuais.get(unidade.getId())))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    private UnidadeResponseDTO toResponseDTO(Unidade unidade, Ocupacao ocupacaoAtual) {
        TipoOcupacao tipo = ocupacaoAtual != null ? ocupacaoAtual.getTipo() : null;
        boolean ocupada = tipo == TipoOcupacao.PROPRIETARIO_MORANDO || tipo == TipoOcupacao.ALUGADA;

        return new UnidadeResponseDTO(
                unidade.getId(), unidade.getNumero(), unidade.getAndar(), unidade.getArea(),
                unidade.getFracaoIdeal(), unidade.getBloco().getId(),
                unidade.getProprietario() != null ? unidade.getProprietario().getId() : null,
                tipo, ocupada
        );
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
}