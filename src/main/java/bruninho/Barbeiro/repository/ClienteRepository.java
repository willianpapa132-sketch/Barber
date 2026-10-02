package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.security.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
}
