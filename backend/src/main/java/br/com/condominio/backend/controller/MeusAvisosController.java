package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.ConfirmacaoLeituraResponseDTO;
import br.com.condominio.backend.dto.MeuAvisoResponseDTO;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.AvisoLeitura;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.AvisoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/meus-avisos")
public class MeusAvisosController {

    private final AvisoService avisoService;
    private final TenantAccessGuard tenantAccessGuard;

    public MeusAvisosController(AvisoService avisoService, TenantAccessGuard tenantAccessGuard) {
        this.avisoService = avisoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @GetMapping
    public ResponseEntity<List<MeuAvisoResponseDTO>> listar(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        List<MeuAvisoResponseDTO> resposta = avisoService
                .listarAvisosDoUsuario(condominioId, usuarioAutenticado.getUsuario())
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    @PreAuthorize("hasAnyRole('MORADOR', 'PROPRIETARIO')")
    @PostMapping("/{avisoId}/leitura")
    public ResponseEntity<ConfirmacaoLeituraResponseDTO> confirmarLeitura(@PathVariable Long condominioId,
                                        @PathVariable Long avisoId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoResidencialAoCondominio(condominioId, usuarioAutenticado);

        AvisoLeitura leitura = avisoService.confirmarLeitura(condominioId, avisoId, usuarioAutenticado.getUsuario());

        return ResponseEntity.ok(new ConfirmacaoLeituraResponseDTO(
                leitura.getAviso().getId(),
                leitura.getUsuario().getId(),
                leitura.getUnidade().getId(),
                leitura.getDataLeitura()));
    }

    private MeuAvisoResponseDTO toResponseDTO(AvisoService.AvisoDoUsuario item) {
        Aviso a = item.aviso();
        return new MeuAvisoResponseDTO(
                a.getId(),
                a.getTitulo(),
                a.getTexto(),
                a.getAlcance(),
                a.getDataValidade(),
                a.getAutor().getNome(),
                a.getDataCriacao(),
                item.dataLeitura() != null,
                item.dataLeitura());
    }
}
