package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendamentoRepository extends JpaRepository<Agendamento,Long> {

    Boolean existsByServicoID(Long id);
}
