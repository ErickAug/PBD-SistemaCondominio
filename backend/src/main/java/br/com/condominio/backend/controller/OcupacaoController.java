package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.OcupacaoRequestDTO;
import br.com.condominio.backend.dto.OcupacaoResponseDTO;
import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.OcupacaoService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/ocupacoes")
public class OcupacaoController {

    private final OcupacaoService ocupacaoService;
    private final BlocoService blocoService;
    private final UnidadeService unidadeService;
    private final TenantAccessGuard tenantAccessGuard;

    public OcupacaoController(OcupacaoService ocupacaoService, BlocoService blocoService,
                              UnidadeService unidadeService, TenantAccessGuard tenantAccessGuard) {
        this.ocupacaoService = ocupacaoService;
        this.blocoService = blocoService;
        this.unidadeService = unidadeService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<OcupacaoResponseDTO> registrar(@PathVariable Long condominioId,
                                                         @PathVariable Long blocoId,
                                                         @PathVariable Long unidadeId,
                                                         @RequestBody OcupacaoRequestDTO dto,
                                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Unidade unidade = validarCadeiaEBuscarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Ocupacao salva = ocupacaoService.registrar(unidade, dto.tipo(), dto.ocupanteId(), dto.dataEntrada());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/atual")
    public ResponseEntity<OcupacaoResponseDTO> atual(@PathVariable Long condominioId,
                                                     @PathVariable Long blocoId,
                                                     @PathVariable Long unidadeId,
                                                     @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarCadeiaEBuscarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Optional<Ocupacao> ocupacao = ocupacaoService.obterAtual(unidadeId);

        return ocupacao.map(o -> ResponseEntity.ok(toResponseDTO(o)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/em-data")
    public ResponseEntity<OcupacaoResponseDTO> emData(@PathVariable Long condominioId,
                                                      @PathVariable Long blocoId,
                                                      @PathVariable Long unidadeId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                                      @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarCadeiaEBuscarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        Optional<Ocupacao> ocupacao = ocupacaoService.obterEmData(unidadeId, data);

        return ocupacao.map(o -> ResponseEntity.ok(toResponseDTO(o)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<OcupacaoResponseDTO>> historico(@PathVariable Long condominioId,
                                                               @PathVariable Long blocoId,
                                                               @PathVariable Long unidadeId,
                                                               @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarCadeiaEBuscarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<OcupacaoResponseDTO> resposta = ocupacaoService.listarHistorico(unidadeId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(resposta);
    }

    private Unidade validarCadeiaEBuscarUnidade(Long condominioId, Long blocoId, Long unidadeId,
                                                UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        blocoService.buscarValidandoCondominio(blocoId, condominioId);
        return unidadeService.buscarValidandoBloco(unidadeId, blocoId);
    }

    private OcupacaoResponseDTO toResponseDTO(Ocupacao ocupacao) {
        return new OcupacaoResponseDTO(
                ocupacao.getId(),
                ocupacao.getUnidade().getId(),
                ocupacao.getTipo(),
                ocupacao.getOcupante() != null ? ocupacao.getOcupante().getId() : null,
                ocupacao.getOcupante() != null ? ocupacao.getOcupante().getNome() : null,
                ocupacao.getDataEntrada()
        );
    }
}