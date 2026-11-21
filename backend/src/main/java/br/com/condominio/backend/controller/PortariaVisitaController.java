package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.VisitaEsperadaDTO;
import br.com.condominio.backend.model.AutorizacaoVisita;
import br.com.condominio.backend.model.AutorizadoPermanente;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AutorizacaoVisitaService;
import br.com.condominio.backend.service.AutorizadoPermanenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/condominios/{condominioId}/portaria/visitas")
public class PortariaVisitaController {

    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final AutorizadoPermanenteService autorizadoPermanenteService;
    private final TenantAccessGuard tenantAccessGuard;

    public PortariaVisitaController(AutorizacaoVisitaService autorizacaoVisitaService,
                                     AutorizadoPermanenteService autorizadoPermanenteService,
                                     TenantAccessGuard tenantAccessGuard) {
        this.autorizacaoVisitaService = autorizacaoVisitaService;
        this.autorizadoPermanenteService = autorizadoPermanenteService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping("/busca")
    public ResponseEntity<List<VisitaEsperadaDTO>> buscar(@PathVariable Long condominioId,
                                        @RequestParam String termo,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        List<AutorizacaoVisita> avulsas = autorizacaoVisitaService.buscarPorNomeOuDocumento(condominioId, termo);
        List<AutorizadoPermanente> permanentes = autorizadoPermanenteService.buscarPorNomeOuDocumento(condominioId, termo);

        List<VisitaEsperadaDTO> resposta = Stream.concat(
                        avulsas.stream().map(this::toDTO),
                        permanentes.stream().map(this::toDTO))
                .sorted(Comparator.comparing(VisitaEsperadaDTO::nome))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping("/hoje")
    public ResponseEntity<List<VisitaEsperadaDTO>> listarEsperadasHoje(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        List<AutorizacaoVisita> avulsas = autorizacaoVisitaService.listarEsperadasHoje(condominioId);
        List<AutorizadoPermanente> permanentes = autorizadoPermanenteService.listarValidosHoje(condominioId);

        List<VisitaEsperadaDTO> resposta = Stream.concat(
                        avulsas.stream().map(this::toDTO),
                        permanentes.stream().map(this::toDTO))
                .sorted(Comparator.comparing(VisitaEsperadaDTO::nome))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<VisitaEsperadaDTO> confirmar(@PathVariable Long condominioId,
                                        @PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        AutorizacaoVisita confirmada = autorizacaoVisitaService.confirmarUso(condominioId, id);

        return ResponseEntity.ok(toDTO(confirmada));
    }

    private VisitaEsperadaDTO toDTO(AutorizacaoVisita a) {
        return new VisitaEsperadaDTO("AVULSA", a.getId(), a.getUnidade().getId(),
                a.getUnidade().getBloco().getNome(), a.getUnidade().getNumero(),
                a.getNome(), a.getDocumento(), a.getTipo(),
                a.getDataInicio(), a.getDataFim(), autorizacaoVisitaService.calcularSituacao(a));
    }

    private VisitaEsperadaDTO toDTO(AutorizadoPermanente a) {
        String situacao = autorizadoPermanenteService.estaLiberadoHoje(a) ? "LIBERADA" : "VENCIDA";

        return new VisitaEsperadaDTO("PERMANENTE", a.getId(), a.getUnidade().getId(),
                a.getUnidade().getBloco().getNome(), a.getUnidade().getNumero(),
                a.getNome(), a.getDocumento(), a.getTipo(),
                a.getDataInicio(), a.getDataFim(), situacao);
    }
}
