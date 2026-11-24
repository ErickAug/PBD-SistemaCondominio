package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.OcorrenciaMoradorDetalheDTO;
import br.com.condominio.backend.dto.OcorrenciaMoradorRequestDTO;
import br.com.condominio.backend.dto.OcorrenciaMoradorResumoDTO;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.OcorrenciaMoradorService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/ocorrencias")
public class OcorrenciaMoradorController {

    private final OcorrenciaMoradorService ocorrenciaMoradorService;
    private final OcorrenciaMoradorMapper mapper;
    private final TenantAccessGuard tenantAccessGuard;
    private final BlocoService blocoService;
    private final UnidadeService unidadeService;

    public OcorrenciaMoradorController(OcorrenciaMoradorService ocorrenciaMoradorService,
                                       OcorrenciaMoradorMapper mapper,
                                       TenantAccessGuard tenantAccessGuard,
                                       BlocoService blocoService,
                                       UnidadeService unidadeService) {
        this.ocorrenciaMoradorService = ocorrenciaMoradorService;
        this.mapper = mapper;
        this.tenantAccessGuard = tenantAccessGuard;
        this.blocoService = blocoService;
        this.unidadeService = unidadeService;
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @PostMapping
    public ResponseEntity<OcorrenciaMoradorDetalheDTO> abrir(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @RequestBody OcorrenciaMoradorRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = validarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        OcorrenciaMorador ocorrencia = ocorrenciaMoradorService.abrir(unidade, usuarioAutenticado.getUsuario(),
                dto.assunto(), dto.tipo(), dto.descricao(), dto.foto());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDetalhe(ocorrencia));
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<OcorrenciaMoradorResumoDTO>> listar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = validarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<OcorrenciaMoradorResumoDTO> resposta = ocorrenciaMoradorService
                .listarDaUnidade(unidade, usuarioAutenticado.getUsuario())
                .stream().map(mapper::toResumo).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @GetMapping("/{ocorrenciaId}")
    public ResponseEntity<OcorrenciaMoradorDetalheDTO> detalhar(@PathVariable Long condominioId,
                                        @PathVariable Long blocoId,
                                        @PathVariable Long unidadeId,
                                        @PathVariable Long ocorrenciaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = validarUnidade(condominioId, blocoId, unidadeId, usuarioAutenticado);

        OcorrenciaMorador ocorrencia = ocorrenciaMoradorService.detalharDaUnidade(unidade,
                usuarioAutenticado.getUsuario(), ocorrenciaId);

        return ResponseEntity.ok(mapper.toDetalhe(ocorrencia));
    }

    private Unidade validarUnidade(Long condominioId, Long blocoId, Long unidadeId,
                                   UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);
        blocoService.buscarValidandoCondominio(blocoId, condominioId);
        return unidadeService.buscarValidandoBloco(unidadeId, blocoId);
    }
}
