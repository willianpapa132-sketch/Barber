package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.MovimentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoCaixaRepository extends JpaRepository<MovimentoCaixa, Long> {
    boolean existsByAgendamento_Id(Long agendamentoId);
    java.util.List<MovimentoCaixa> findByCaixa_IdOrderByCriadoEmDescIdDesc(Long caixaId);
}
