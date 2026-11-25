package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.CobrancaResponseDTO;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UnidadeAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.CobrancaService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/blocos/{blocoId}/unidades/{unidadeId}/cobrancas")
public class CobrancaUnidadeController {

    private final CobrancaService cobrancaService;
    private final UnidadeAccessGuard unidadeAccessGuard;
    private final TenantAccessGuard tenantAccessGuard;
    private final BlocoService blocoService;
    private final UnidadeService unidadeService;

    public CobrancaUnidadeController(CobrancaService cobrancaService, UnidadeAccessGuard unidadeAccessGuard,
                                     TenantAccessGuard tenantAccessGuard, BlocoService blocoService,
                                     UnidadeService unidadeService) {
        this.cobrancaService = cobrancaService;
        this.unidadeAccessGuard = unidadeAccessGuard;
        this.tenantAccessGuard = tenantAccessGuard;
        this.blocoService = blocoService;
        this.unidadeService = unidadeService;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<CobrancaResponseDTO>> listar(
            @PathVariable Long condominioId, @PathVariable Long blocoId, @PathVariable Long unidadeId,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Unidade unidade = validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);

        List<CobrancaResponseDTO> lista = cobrancaService.listarPorUnidade(unidade.getId())
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(lista);
    }

    private Unidade validarAcesso(Long condominioId, Long blocoId, Long unidadeId,
                                  UsuarioDetailsImpl usuarioAutenticado) {
        Usuario usuario = usuarioAutenticado.getUsuario();

        if (usuario.getPerfil() == Perfil.PROPRIETARIO) {
            tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);
            blocoService.buscarValidandoCondominio(blocoId, condominioId);
            Unidade unidade = unidadeService.buscarValidandoBloco(unidadeId, blocoId);

            boolean ehODono = unidade.getProprietario() != null
                    && unidade.getProprietario().getId().equals(usuario.getId());

            if (ehODono) {
                return unidade;
            }
        }

        return unidadeAccessGuard.validarAcesso(condominioId, blocoId, unidadeId, usuarioAutenticado);
    }

    private CobrancaResponseDTO toResponseDTO(Cobranca cobranca) {
        var rateioUnidade = cobranca.getRateioUnidade();
        var unidade = rateioUnidade.getUnidade();
        boolean emAtraso = cobranca.isEmAtraso();
        Long diasAtraso = emAtraso ? ChronoUnit.DAYS.between(cobranca.getVencimento(), LocalDate.now()) : null;

        return new CobrancaResponseDTO(
                cobranca.getId(), unidade.getId(), unidade.getNumero(),
                unidade.getAndar(), unidade.getBloco().getNome(),
                rateioUnidade.getRateioMensal().getCompetencia(), cobranca.getVencimento(),
                cobranca.getSituacao(), emAtraso, diasAtraso,
                rateioUnidade.getValorOrdinario(), rateioUnidade.getValorExtraordinario(), rateioUnidade.getValorTotal(),
                rateioUnidade.getResponsavelOrdinario() != null ? rateioUnidade.getResponsavelOrdinario().getNome() : null,
                rateioUnidade.getResponsavelExtraordinario() != null ? rateioUnidade.getResponsavelExtraordinario().getNome() : null,
                cobranca.getValorPago(), cobranca.getDataPagamento(), cobranca.getFormaPagamento(),
                cobranca.getDiferenca(),
                cobranca.getBaixadoPor() != null ? cobranca.getBaixadoPor().getNome() : null
        );
    }
}