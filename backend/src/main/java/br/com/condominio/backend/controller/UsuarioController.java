package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UsuarioRequestDTO;
import br.com.condominio.backend.dto.UsuarioResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
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
@RequestMapping("/administradoras/{administradoraId}/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private TenantAccessGuard tenantAccessGuard;

    public UsuarioController(UsuarioService usuarioService, TenantAccessGuard tenantAccessGuard) {
        this.usuarioService = usuarioService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getUsuario(),
                usuario.getPerfil(),
                usuario.getAdministradora() != null ? usuario.getAdministradora().getId() : null,
                usuario.getCondominio() != null ? usuario.getCondominio().getId() : null,
                usuario.getContato()
        );
    }

    @PreAuthorize("hasRole('ADMINISTRADORA')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@PathVariable Long administradoraId,
                                                        @RequestBody UsuarioRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdAutenticado = tenantAccessGuard.validarAdministradora(administradoraId, usuarioAutenticado);

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setUsuario(dto.usuario());
        usuario.setSenha(dto.senha());
        usuario.setPerfil(dto.perfil());

        Usuario salvo = usuarioService.cadastrarAdministrador(usuario, administradoraIdAutenticado);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

}