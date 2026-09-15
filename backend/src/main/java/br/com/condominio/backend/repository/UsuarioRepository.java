package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByUsuario(String usuario);

    Optional<Usuario> findByUsuario(String usuario);

    boolean existsByUnidadeId(Long unidadeId);

    @Query("SELECT DISTINCT u.unidade.id FROM Usuario u WHERE u.unidade.id IN :unidadeIds")
    List<Long> buscarIdsDeUnidadesOcupadas(@Param("unidadeIds") List<Long> unidadeIds);
}