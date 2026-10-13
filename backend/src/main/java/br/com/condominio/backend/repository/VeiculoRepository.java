package br.com.condominio.backend.repository;

import br.com.condominio.backend.model.Veiculo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @EntityGraph(attributePaths = {"unidade", "unidade.bloco", "dono", "vaga"})
    Optional<Veiculo> findFirstByCondominioIdAndPlacaAndAtivoTrue(Long condominioId, String placa);

    List<Veiculo> findByUnidadeIdAndAtivoTrue(Long unidadeId);

    List<Veiculo> findByUnidadeIdAndDonoIdAndAtivoTrue(Long unidadeId, Long donoId);

    boolean existsByVagaId(Long vagaId);

    List<Veiculo> findByVagaIdIn(List<Long> vagaIds);
}