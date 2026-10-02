package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento,Long> {

    Boolean existsByServicoID(Long id);


    List<Agendamento> findAllByBarbeiro_idAndData(Long barbeiro_id , LocalDate data);
}
