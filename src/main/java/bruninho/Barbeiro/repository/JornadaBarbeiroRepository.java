package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.JornadaBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface JornadaBarbeiroRepository extends JpaRepository<JornadaBarbeiro,Long> {

    Optional <JornadaBarbeiro> findByDiaSemanaAndBarbeiro_id(DayOfWeek diaSemana, Long barbeiro_Id);
}
