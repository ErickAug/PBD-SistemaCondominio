package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.RelatorioPeriodoDTO;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/condominios/{condominioId}/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final TenantAccessGuard tenantAccessGuard;

    public RelatorioController(RelatorioService relatorioService, TenantAccessGuard tenantAccessGuard) {
        this.relatorioService = relatorioService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<RelatorioPeriodoDTO> gerar(
            @PathVariable Long condominioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        RelatorioPeriodoDTO relatorio = relatorioService.gerarRelatorio(condominioId, competencia);

        return ResponseEntity.ok(relatorio);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'MORADOR', 'PROPRIETARIO')")
    @GetMapping("/csv")
    public ResponseEntity<byte[]> gerarCsv(
            @PathVariable Long condominioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        RelatorioPeriodoDTO relatorio = relatorioService.gerarRelatorio(condominioId, competencia);
        byte[] csv = relatorioService.gerarCsv(relatorio).getBytes(StandardCharsets.UTF_8);

        String nomeArquivo = "prestacao-contas-" + competencia + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }
}
