package bruninho.Barbeiro.security.repository;

import bruninho.Barbeiro.security.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario,Long> {


    Optional<Usuario> findByLogin(String login);
}
