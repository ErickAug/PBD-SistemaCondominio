package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UsuarioCondominioRequestDTO;
import br.com.condominio.backend.dto.UsuarioResponseDTO;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.security.TenantAccessGuard;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/condominios/{condominioId}/usuarios")
public class UsuarioCondominioController {

    private final UsuarioService usuarioService;
    private final TenantAccessGuard tenantAccessGuard;

    public UsuarioCondominioController(UsuarioService usuarioService, TenantAccessGuard tenantAccessGuard) {
        this.usuarioService = usuarioService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADORA', 'SINDICO')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@PathVariable Long condominioId,
                                                        @RequestBody UsuarioCondominioRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Condominio condominio = tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setUsuario(dto.usuario());
        usuario.setSenha(dto.senha());
        usuario.setPerfil(dto.perfil());

        Usuario salvo = usuarioService.cadastrarNoCondominio(
                usuario, condominio, usuarioAutenticado.getUsuario().getPerfil());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(), usuario.getNome(), usuario.getUsuario(), usuario.getPerfil(),
                usuario.getAdministradora() != null ? usuario.getAdministradora().getId() : null,
                usuario.getCondominio() != null ? usuario.getCondominio().getId() : null
        );
    }
}