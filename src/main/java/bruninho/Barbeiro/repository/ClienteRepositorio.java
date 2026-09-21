package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
    List<Cliente> findTop30ByNomeContainingIgnoreCaseOrTelefoneNormalizadoContainingOrderByNome(String nome, String telefone);
    Optional<Cliente> findFirstByTelefoneNormalizadoOrderByIdAsc(String telefoneNormalizado);
}
