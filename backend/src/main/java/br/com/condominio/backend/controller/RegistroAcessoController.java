package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.RegistroAcessoManualRequestDTO;
import br.com.condominio.backend.dto.RegistroAcessoResponseDTO;
import br.com.condominio.backend.model.RegistroAcesso;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.RegistroAcessoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/portaria/registros")
public class RegistroAcessoController {

    private final RegistroAcessoService registroAcessoService;
    private final TenantAccessGuard tenantAccessGuard;

    public RegistroAcessoController(RegistroAcessoService registroAcessoService,
                                     TenantAccessGuard tenantAccessGuard) {
        this.registroAcessoService = registroAcessoService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/manual")
    public ResponseEntity<RegistroAcessoResponseDTO> registrarManual(@PathVariable Long condominioId,
                                        @RequestBody RegistroAcessoManualRequestDTO dto,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        RegistroAcesso registro = registroAcessoService.registrarManual(condominioId, dto.unidadeId(), dto.nome(),
                dto.documento(), dto.tipo(), dto.quemAutorizou(), dto.placa(),
                usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(registro));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/autorizacao-visita/{autorizacaoVisitaId}")
    public ResponseEntity<RegistroAcessoResponseDTO> registrarPorAutorizacaoVisita(@PathVariable Long condominioId,
                                        @PathVariable Long autorizacaoVisitaId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        RegistroAcesso registro = registroAcessoService.registrarPorAutorizacaoVisita(condominioId,
                autorizacaoVisitaId, usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(registro));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @PostMapping("/autorizado-permanente/{autorizadoPermanenteId}")
    public ResponseEntity<RegistroAcessoResponseDTO> registrarPorAutorizadoPermanente(@PathVariable Long condominioId,
                                        @PathVariable Long autorizadoPermanenteId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        RegistroAcesso registro = registroAcessoService.registrarPorAutorizadoPermanente(condominioId,
                autorizadoPermanenteId, usuarioAutenticado.getUsuario());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(registro));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO', 'PORTARIA')")
    @GetMapping
    public ResponseEntity<List<RegistroAcessoResponseDTO>> listar(@PathVariable Long condominioId,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {

        tenantAccessGuard.validarAcessoLeituraAoCondominio(condominioId, usuarioAutenticado);

        List<RegistroAcessoResponseDTO> resposta = registroAcessoService.listarPorCondominio(condominioId)
                .stream().map(this::toResponseDTO).toList();

        return ResponseEntity.ok(resposta);
    }

    private RegistroAcessoResponseDTO toResponseDTO(RegistroAcesso r) {
        return new RegistroAcessoResponseDTO(r.getId(), r.getUnidade().getId(),
                r.getUnidade().getBloco().getNome(), r.getUnidade().getNumero(),
                r.getNome(), r.getDocumento(), r.getTipo(), r.getOrigem(), r.getQuemAutorizou(),
                r.getVeiculo() != null ? r.getVeiculo().getPlaca() : null,
                r.getPorteiro().getNome(),
                r.getHoraEntrada(), r.getHoraSaida());
    }
}
