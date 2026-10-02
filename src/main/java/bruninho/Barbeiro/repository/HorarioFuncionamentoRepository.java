package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    HorarioFuncionamento findByDiaSemana(DayOfWeek diaSemana);

}
