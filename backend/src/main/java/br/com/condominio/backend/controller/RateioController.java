package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.RateioResponseDTO;
import br.com.condominio.backend.dto.RateioUnidadeResponseDTO;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.RateioUnidade;
import br.com.condominio.backend.model.FechamentoMensal;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.FechamentoMensalService;
import br.com.condominio.backend.service.RateioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/rateios")
public class RateioController {

    private final RateioService rateioService;
    private final TenantAccessGuard tenantAccessGuard;

    public RateioController(RateioService rateioService, TenantAccessGuard tenantAccessGuard) {
        this.rateioService = rateioService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping("/previa")
    public ResponseEntity<RateioResponseDTO> previa(
            @PathVariable Long condominioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        List<RateioUnidade> itens = rateioService.calcular(condominio, competencia);

        return ResponseEntity.ok(toResponseDTO(null, competencia.withDayOfMonth(1), itens));
    }

    private RateioResponseDTO toResponseDTO(Long id, LocalDate competencia, List<RateioUnidade> itens) {
        BigDecimal totalOrdinario = itens.stream().map(RateioUnidade::getValorOrdinario).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExtraordinario = itens.stream().map(RateioUnidade::getValorExtraordinario).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RateioUnidadeResponseDTO> unidades = itens.stream().map(item -> new RateioUnidadeResponseDTO(
                item.getUnidade().getId(), item.getUnidade().getNumero(),
                item.getValorOrdinario(), item.getValorExtraordinario(), item.getValorTotal(),
                item.getResponsavelOrdinario() != null ? item.getResponsavelOrdinario().getNome() : null,
                item.getResponsavelExtraordinario() != null ? item.getResponsavelExtraordinario().getNome() : null
        )).toList();

        return new RateioResponseDTO(id, competencia, totalOrdinario, totalExtraordinario, unidades);
    }
}