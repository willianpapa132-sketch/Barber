package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.StatusAgendamento;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepositorio extends JpaRepository<Agendamento, Long> {
    @Query("""
        select a from Agendamento a
        where a.barbeiro.id = :barbeiroId
          and a.status in :blockingStatuses
          and a.inicioEm < :fimEm
          and a.fimEm > :inicioEm
        order by a.inicioEm
    """)
    List<Agendamento> findConflicts(@Param("barbeiroId") Long barbeiroId,
                                    @Param("inicioEm") LocalDateTime inicioEm,
                                    @Param("fimEm") LocalDateTime fimEm,
                                    @Param("blockingStatuses") Collection<StatusAgendamento> blockingStatuses);

    @Query("""
        select a from Agendamento a
        where a.barbeiro.id = :barbeiroId and a.inicioEm >= :from and a.inicioEm < :to
        order by a.inicioEm
    """)
    List<Agendamento> findAgenda(@Param("barbeiroId") Long barbeiroId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select a from Agendamento a
        where (:barbeiroId is null or a.barbeiro.id = :barbeiroId)
          and a.inicioEm >= :from and a.inicioEm < :to
          and (:status is null or a.status = :status)
        order by a.inicioEm
    """)
    List<Agendamento> search(@Param("barbeiroId") Long barbeiroId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("status") StatusAgendamento status);

    List<Agendamento> findByClienteIdOrderByInicioEmDesc(Long clienteId);
    long countByPlanoMensalIdAndStatusInAndInicioEmGreaterThanEqualAndInicioEmLessThan(Long planoMensalId, Collection<StatusAgendamento> statuses, LocalDateTime from, LocalDateTime to);
    long countByStatusAndInicioEmBetween(StatusAgendamento status, LocalDateTime from, LocalDateTime to);
    List<Agendamento> findTop20ByInicioEmBetweenOrderByInicioEm(LocalDateTime from, LocalDateTime to);
}
