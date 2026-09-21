package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.MovimentoCaixa;
import bruninho.Barbeiro.domain.TipoMovimentoCaixa;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoCaixaRepositorio extends JpaRepository<MovimentoCaixa, Long> {
    Optional<MovimentoCaixa> findByAgendamentoIdAndTipoAndEstornadoFalse(Long agendamentoId, TipoMovimentoCaixa type);
    List<MovimentoCaixa> findByCriadoEmBetweenOrderByCriadoEmDesc(LocalDateTime from, LocalDateTime to);
    List<MovimentoCaixa> findBySessaoCaixaIdOrderByCriadoEm(Long sessaoCaixaId);
}
