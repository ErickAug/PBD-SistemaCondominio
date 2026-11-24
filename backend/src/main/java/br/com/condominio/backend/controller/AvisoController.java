package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AvisoLeiturasResponseDTO;
import br.com.condominio.backend.dto.AvisoRequestDTO;
import br.com.condominio.backend.dto.AvisoResponseDTO;
import br.com.condominio.backend.dto.LeituraDestinatarioDTO;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AvisoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/avisos")
public class AvisoController {

    private final AvisoService avisoService;
    private final TenantAccessGuard tenantAccessGuard;

    public AvisoController(AvisoService avisoService, TenantAccessGuard tenantAccessGuard) {
        this.avisoService = avisoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<AvisoResponseDTO> criar(@PathVariable Long condominioId,
                                        @RequestBody AvisoRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Aviso aviso = avisoService.criar(condominio, usuarioAutenticado.getUsuario(), dto.titulo(), dto.texto(),
                dto.alcance(), dto.blocoId(), dto.unidadeId(), dto.dataValidade());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(aviso));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<List<AvisoResponseDTO>> listarHistorico(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        List<AvisoResponseDTO> resposta = avisoService.listarHistorico(condominioId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/{avisoId}/leituras")
    public ResponseEntity<AvisoLeiturasResponseDTO> listarLeituras(@PathVariable Long condominioId,
                                        @PathVariable Long avisoId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        AvisoService.RelatorioLeitura relatorio = avisoService.gerarRelatorioLeitura(condominioId, avisoId);

        List<LeituraDestinatarioDTO> leram = relatorio.leram().stream().map(this::toLeituraDTO).toList();
        List<LeituraDestinatarioDTO> naoLeram = relatorio.naoLeram().stream().map(this::toLeituraDTO).toList();

        return ResponseEntity.ok(new AvisoLeiturasResponseDTO(
                relatorio.aviso().getId(),
                relatorio.aviso().getTitulo(),
                avisoService.estaVencido(relatorio.aviso()),
                leram.size() + naoLeram.size(),
                leram.size(),
                naoLeram.size(),
                leram,
                naoLeram));
    }

    private LeituraDestinatarioDTO toLeituraDTO(AvisoService.SituacaoLeitura s) {
        return new LeituraDestinatarioDTO(
                s.usuario().getId(),
                s.usuario().getNome(),
                s.unidade() != null ? s.unidade().getId() : null,
                s.unidade() != null ? s.unidade().getBloco().getNome() : null,
                s.unidade() != null ? s.unidade().getNumero() : null,
                s.papel(),
                s.dataLeitura() != null,
                s.dataLeitura());
    }

    private AvisoResponseDTO toResponseDTO(Aviso a) {
        return new AvisoResponseDTO(
                a.getId(),
                a.getCondominio().getId(),
                a.getTitulo(),
                a.getTexto(),
                a.getAlcance(),
                a.getBloco() != null ? a.getBloco().getId() : null,
                a.getBloco() != null ? a.getBloco().getNome() : null,
                a.getUnidade() != null ? a.getUnidade().getId() : null,
                a.getUnidade() != null ? a.getUnidade().getNumero() : null,
                a.getDataValidade(),
                avisoService.estaVencido(a),
                a.getAutor().getNome(),
                a.getDataCriacao());
    }
}
