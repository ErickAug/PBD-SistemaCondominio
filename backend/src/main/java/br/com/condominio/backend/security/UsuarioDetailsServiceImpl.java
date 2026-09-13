package br.com.condominio.backend.security;

import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usuarioLogin) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsuario(usuarioLogin)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return new UsuarioDetailsImpl(usuario);
    }
}