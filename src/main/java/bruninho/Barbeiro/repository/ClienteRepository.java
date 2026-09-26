package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
}
