package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Servico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepositorio extends JpaRepository<Servico, Long> {
    List<Servico> findByAtivoTrueOrderByNome();
    List<Servico> findAllByOrderByNome();
}
