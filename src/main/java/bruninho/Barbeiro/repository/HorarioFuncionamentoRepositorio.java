package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.HorarioFuncionamento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioFuncionamentoRepositorio extends JpaRepository<HorarioFuncionamento, Long> {
    Optional<HorarioFuncionamento> findByDiaSemana(int diaSemana);
}
