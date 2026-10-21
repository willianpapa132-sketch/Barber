package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoBarbeariaRepository extends JpaRepository<ConfiguracaoBarbearia,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from ConfiguracaoBarbearia c where c.id=1")
    java.util.Optional<ConfiguracaoBarbearia> bloquear();
}
