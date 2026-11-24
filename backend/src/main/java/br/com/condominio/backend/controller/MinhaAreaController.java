package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AvisoPainelDTO;
import br.com.condominio.backend.dto.CobrancaPainelDTO;
import br.com.condominio.backend.dto.MinhaUnidadeDTO;
import br.com.condominio.backend.dto.PainelUnidadeDTO;
import br.com.condominio.backend.dto.PermissoesPainelDTO;
import br.com.condominio.backend.dto.SituacaoFinanceiraDTO;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.MinhaAreaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/minha-area")
public class MinhaAreaController {

    private final MinhaAreaService minhaAreaService;
    private final TenantAccessGuard tenantAccessGuard;
    private final ReservaMapper reservaMapper;
    private final OcorrenciaMoradorMapper ocorrenciaMapper;

    public MinhaAreaController(MinhaAreaService minhaAreaService, TenantAccessGuard tenantAccessGuard,
                               ReservaMapper reservaMapper, OcorrenciaMoradorMapper ocorrenciaMapper) {
        this.minhaAreaService = minhaAreaService;
        this.tenantAccessGuard = tenantAccessGuard;
        this.reservaMapper = reservaMapper;
        this.ocorrenciaMapper = ocorrenciaMapper;
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @GetMapping("/unidades")
    public ResponseEntity<List<MinhaUnidadeDTO>> listarUnidades(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        List<MinhaUnidadeDTO> resposta = minhaAreaService
                .listarMinhasUnidades(condominioId, usuarioAutenticado.getUsuario()).stream()
                .map(m -> toUnidadeDTO(m.unidade(), papeis(m.mora(), m.dono())))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @GetMapping("/unidades/{unidadeId}")
    public ResponseEntity<PainelUnidadeDTO> painel(@PathVariable Long condominioId,
                                        @PathVariable Long unidadeId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        MinhaAreaService.Painel p = minhaAreaService.montarPainel(condominioId, unidadeId,
                usuarioAutenticado.getUsuario(), competencia);

        MinhaAreaService.Permissoes perm = p.permissoes();
        MinhaAreaService.SituacaoFinanceira sf = p.situacaoFinanceira();

        return ResponseEntity.ok(new PainelUnidadeDTO(
                toUnidadeDTO(p.unidade(), p.papeis()),
                new PermissoesPainelDTO(perm.autorizarVisita(), perm.reservarAreaComum(), perm.abrirOcorrencia(),
                        perm.verPrestacaoContas()),
                p.avisos().stream().map(this::toAvisoDTO).toList(),
                p.cobrancas().stream().map(this::toCobrancaDTO).toList(),
                new SituacaoFinanceiraDTO(sf.emDia(), sf.cobrancasEmAtraso(), sf.valorEmAtraso()),
                p.reservas() != null ? p.reservas().stream().map(reservaMapper::toReservaDTO).toList() : null,
                p.ocorrencias().stream().map(ocorrenciaMapper::toResumo).toList(),
                p.competencia(),
                p.prestacaoContas()));
    }

    private List<String> papeis(boolean mora, boolean dono) {
        List<String> papeis = new ArrayList<>();
        if (mora) {
            papeis.add(MinhaAreaService.PAPEL_MORADOR);
        }
        if (dono) {
            papeis.add(MinhaAreaService.PAPEL_PROPRIETARIO);
        }
        return papeis;
    }

    private MinhaUnidadeDTO toUnidadeDTO(Unidade u, List<String> papeis) {
        return new MinhaUnidadeDTO(u.getId(), u.getBloco().getId(), u.getBloco().getNome(), u.getNumero(), papeis);
    }

    private AvisoPainelDTO toAvisoDTO(MinhaAreaService.AvisoDoPainel item) {
        Aviso a = item.aviso();
        return new AvisoPainelDTO(a.getId(), a.getTitulo(), a.getTexto(), a.getAlcance(), a.getDataValidade(),
                a.getAutor().getNome(), a.getDataCriacao(), item.confirmavel(), item.dataLeitura() != null,
                item.dataLeitura());
    }

    private CobrancaPainelDTO toCobrancaDTO(MinhaAreaService.CobrancaDoPainel c) {
        return new CobrancaPainelDTO(c.cobranca().getId(), c.competencia(), c.cobranca().getVencimento(),
                c.cobranca().getSituacao(), c.emAtraso(), c.diasAtraso(), c.parteOrdinaria(),
                c.parteExtraordinaria(), c.valorTotal(), c.cobranca().getDataPagamento());
    }
}
