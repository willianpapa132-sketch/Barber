package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Barbeiro;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarbeiroRepositorio extends JpaRepository<Barbeiro, Long> {
    List<Barbeiro> findByAtivoTrueOrderByNome();
    List<Barbeiro> findAllByOrderByNome();
    Optional<Barbeiro> findByUsuarioLogin(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    Optional<Barbeiro> lockById(@Param("id") Long id);

    @Query("select distinct b from Barbeiro b join b.servicos s where b.ativo = true and s.id = :servicoId and s.ativo = true order by b.nome")
    List<Barbeiro> findAtivosByServico(@Param("servicoId") Long servicoId);
}
