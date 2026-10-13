package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    Optional<Servico> findByNomeIgnoreCase(String nome);
}
