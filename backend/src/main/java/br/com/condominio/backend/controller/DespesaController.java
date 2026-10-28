package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.DespesaRequestDTO;
import br.com.condominio.backend.dto.DespesaResponseDTO;
import br.com.condominio.backend.dto.DespesasListaResponseDTO;
import br.com.condominio.backend.dto.ParcelaDespesaResponseDTO;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Despesa;
import br.com.condominio.backend.model.ParcelaDespesa;
import br.com.condominio.backend.model.enums.TipoDespesa;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.DespesaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/despesas")
public class DespesaController {

    private final DespesaService despesaService;
    private final TenantAccessGuard tenantAccessGuard;

    public DespesaController(DespesaService despesaService, TenantAccessGuard tenantAccessGuard) {
        this.despesaService = despesaService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<DespesaResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                        @RequestBody DespesaRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Despesa despesa = new Despesa();
        preencher(despesa, dto);

        Despesa salva = despesaService.cadastrar(despesa, dto.numeroParcelas(), condominio);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salva));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PutMapping("/{despesaId}")
    public ResponseEntity<DespesaResponseDTO> atualizar(@PathVariable Long condominioId,
                                                        @PathVariable Long despesaId,
                                                        @RequestBody DespesaRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Despesa dadosNovos = new Despesa();
        preencher(dadosNovos, dto);

        Despesa atualizada = despesaService.atualizar(despesaId, dadosNovos, dto.numeroParcelas(), condominioId);

        return ResponseEntity.ok(toResponseDTO(atualizada));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @DeleteMapping("/{despesaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long condominioId,
                                        @PathVariable Long despesaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        despesaService.excluir(despesaId, condominioId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @GetMapping
    public ResponseEntity<DespesasListaResponseDTO> listar(
            @PathVariable Long condominioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) TipoDespesa tipo,
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        DespesaService.ResultadoListagem resultado =
                despesaService.listar(condominioId, dataInicio, dataFim, categoria, tipo);

        List<DespesaResponseDTO> lista = resultado.despesas().stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new DespesasListaResponseDTO(lista, resultado.total()));
    }

    private void preencher(Despesa despesa, DespesaRequestDTO dto) {
        despesa.setData(dto.data());
        despesa.setCategoria(dto.categoria());
        despesa.setFornecedor(dto.fornecedor());
        despesa.setValor(dto.valor());
        despesa.setTipo(dto.tipo());
        despesa.setComprovante(dto.comprovante());
    }

    private DespesaResponseDTO toResponseDTO(Despesa despesa) {
        List<ParcelaDespesaResponseDTO> parcelas = despesa.getTipo() == TipoDespesa.EXTRAORDINARIA
                ? despesaService.listarParcelas(despesa.getId()).stream().map(this::toParcelaDTO).toList()
                : List.of();

        return new DespesaResponseDTO(
                despesa.getId(), despesa.getData(), despesa.getCategoria(), despesa.getFornecedor(),
                despesa.getValor(), despesa.getTipo(), despesa.getComprovante(),
                despesa.getCondominio().getId(), parcelas
        );
    }

    private ParcelaDespesaResponseDTO toParcelaDTO(ParcelaDespesa parcela) {
        return new ParcelaDespesaResponseDTO(parcela.getId(), parcela.getNumeroParcela(),
                parcela.getValor(), parcela.getCompetencia());
    }
}