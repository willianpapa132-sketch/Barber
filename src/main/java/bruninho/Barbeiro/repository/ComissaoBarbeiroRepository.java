package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ComissaoBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComissaoBarbeiroRepository extends JpaRepository<ComissaoBarbeiro, Long> {
    boolean existsByAgendamento_Id(Long agendamentoId);
}
