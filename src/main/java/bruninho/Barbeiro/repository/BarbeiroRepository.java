package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Barbeiro b where b.id = :id")
    java.util.Optional<Barbeiro> findByIdParaAgendar(@org.springframework.data.repository.query.Param("id") Long id);
}
