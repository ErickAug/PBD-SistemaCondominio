package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByUsuario(String usuario);

    Optional<Usuario> findByUsuario(String usuario);
}