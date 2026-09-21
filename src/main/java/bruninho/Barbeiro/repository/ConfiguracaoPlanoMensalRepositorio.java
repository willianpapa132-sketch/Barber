package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ConfiguracaoPlanoMensal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConfiguracaoPlanoMensalRepositorio extends JpaRepository<ConfiguracaoPlanoMensal, Long> {
    Optional<ConfiguracaoPlanoMensal> findByBarbeiroId(Long barbeiroId);

    @Query("select c from ConfiguracaoPlanoMensal c join fetch c.barbeiro b order by b.nome")
    List<ConfiguracaoPlanoMensal> buscarTodasComBarbeiroOrdenadasPorNome();

    @Query("select c from ConfiguracaoPlanoMensal c join fetch c.barbeiro b where c.ativo = true and b.ativo = true order by b.nome")
    List<ConfiguracaoPlanoMensal> buscarPlanosPublicosAtivos();
}
