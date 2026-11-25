package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AtaRequestDTO;
import br.com.condominio.backend.dto.AtaResponseDTO;
import br.com.condominio.backend.dto.PautaItemDTO;
import br.com.condominio.backend.dto.PresencaRequestDTO;
import br.com.condominio.backend.dto.PresencaResponseDTO;
import br.com.condominio.backend.dto.ReuniaoDetalheDTO;
import br.com.condominio.backend.dto.ReuniaoRequestDTO;
import br.com.condominio.backend.dto.ReuniaoResumoDTO;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Reuniao;
import br.com.condominio.backend.model.ReuniaoPresenca;
import br.com.condominio.backend.model.enums.SituacaoReuniao;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.ReuniaoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/reunioes")
public class ReuniaoController {

    private final ReuniaoService reuniaoService;
    private final TenantAccessGuard tenantAccessGuard;

    public ReuniaoController(ReuniaoService reuniaoService, TenantAccessGuard tenantAccessGuard) {
        this.reuniaoService = reuniaoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<ReuniaoDetalheDTO> convocar(@PathVariable Long condominioId,
                                        @RequestBody ReuniaoRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Reuniao reuniao = reuniaoService.convocar(condominio, usuarioAutenticado.getUsuario(), dto.titulo(),
                dto.data(), dto.hora(), dto.local(), dto.pauta());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDetalhe(reuniao));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<ReuniaoResumoDTO>> listar(@PathVariable Long condominioId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        List<ReuniaoResumoDTO> resposta = reuniaoService.listarHistorico(condominioId, inicio, fim).stream()
                .map(r -> new ReuniaoResumoDTO(r.getId(), r.getTitulo(), r.getData(), r.getHora(), r.getLocal(),
                        r.getSituacao(), reuniaoService.contarItensPauta(r.getId()),
                        reuniaoService.contarPresencas(r.getId()), r.getSituacao() == SituacaoReuniao.REALIZADA))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping("/{reuniaoId}")
    public ResponseEntity<ReuniaoDetalheDTO> detalhar(@PathVariable Long condominioId,
                                        @PathVariable Long reuniaoId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        return ResponseEntity.ok(toDetalhe(reuniaoService.buscarNoCondominio(condominioId, reuniaoId)));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping("/{reuniaoId}/presencas")
    public ResponseEntity<PresencaResponseDTO> registrarPresenca(@PathVariable Long condominioId,
                                        @PathVariable Long reuniaoId,
                                        @RequestBody PresencaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        ReuniaoPresenca presenca = reuniaoService.registrarPresenca(condominioId, reuniaoId, dto.unidadeId(),
                dto.compareceu(), usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toPresencaDTO(presenca));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping("/{reuniaoId}/ata")
    public ResponseEntity<ReuniaoDetalheDTO> anexarAta(@PathVariable Long condominioId,
                                        @PathVariable Long reuniaoId,
                                        @RequestBody AtaRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Reuniao reuniao = reuniaoService.anexarAta(condominioId, reuniaoId, dto.texto(), dto.arquivo(),
                usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(toDetalhe(reuniao));
    }

    private ReuniaoDetalheDTO toDetalhe(Reuniao r) {
        List<PautaItemDTO> pauta = reuniaoService.listarPauta(r.getId()).stream()
                .map(i -> new PautaItemDTO(i.getOrdem(), i.getDescricao()))
                .toList();

        List<PresencaResponseDTO> presencas = reuniaoService.listarPresencas(r.getId()).stream()
                .map(this::toPresencaDTO)
                .toList();

        AtaResponseDTO ata = r.getSituacao() == SituacaoReuniao.REALIZADA
                ? new AtaResponseDTO(r.getAtaTexto(), r.getAtaArquivo() != null, r.getAtaArquivo(),
                        r.getAtaAnexadaPor().getNome(), r.getDataAnexoAta())
                : null;

        return new ReuniaoDetalheDTO(
                r.getId(),
                r.getCondominio().getId(),
                r.getTitulo(),
                r.getData(),
                r.getHora(),
                r.getLocal(),
                r.getSituacao(),
                r.getConvocadaPor().getNome(),
                r.getDataPublicacao(),
                pauta,
                presencas,
                presencas.size(),
                reuniaoService.contarUnidadesDoCondominio(r.getCondominio().getId()),
                ata);
    }

    private PresencaResponseDTO toPresencaDTO(ReuniaoPresenca p) {
        return new PresencaResponseDTO(
                p.getId(),
                p.getReuniao().getId(),
                p.getUnidade().getId(),
                p.getUnidade().getBloco().getNome(),
                p.getUnidade().getNumero(),
                p.getUnidade().getAndar(),
                p.getCompareceu(),
                p.getRegistradoPor().getNome(),
                p.getDataRegistro());
    }
}
