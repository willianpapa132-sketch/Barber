package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.JornadaBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JornadaBarbeiroRepository extends JpaRepository<JornadaBarbeiro,Long> {
}
