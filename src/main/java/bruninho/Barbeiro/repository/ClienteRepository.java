package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuario_Login(String login);
}
