package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.StatusAgendamento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento,Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cliente", "servico"})
    @Query("""
            select a from Agendamento a where a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            and a.pagamentoRecebido = true and a.precoTotal > 0
            and a.finalizadoEm >= :inicio and a.finalizadoEm < :fim
            order by a.finalizadoEm desc, a.id desc
            """)
    List<Agendamento> entradasFinanceiro(@Param("inicio") java.time.LocalDateTime inicio,
                                       @Param("fim") java.time.LocalDateTime fim);
    long countByData(LocalDate data);

    @Query("select coalesce(sum(a.precoTotal), 0) from Agendamento a where a.data = :data and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO")
    java.math.BigDecimal somarValoresConcluidos(@Param("data") LocalDate data);

    Optional<Agendamento> findByIdAndBarbeiro_Usuario_Login(Long id, String login);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Agendamento a where a.id = :id and a.barbeiro.usuario.login = :login")
    Optional<Agendamento> findParaFinalizar(@Param("id") Long id, @Param("login") String login);

    @Query("""
            select a from Agendamento a
            where a.barbeiro.id = :barbeiroId
              and a.data = :data
              and (:status is null or a.status = :status)
            order by a.horaInicio asc, a.id asc
            """)
    Page<Agendamento> findAgendaDoBarbeiro(
            @Param("barbeiroId") Long barbeiroId,
            @Param("data") LocalDate data,
            @Param("status") StatusAgendamento status,
            Pageable pageable
    );

    long countByBarbeiro_IdAndData(Long barbeiroId, LocalDate data);

    long countByBarbeiro_IdAndDataAndStatus(Long barbeiroId, LocalDate data, StatusAgendamento status);

    @Query("""
            select a from Agendamento a
            where a.barbeiro.id = :barbeiroId
              and a.data = :data
              and a.horaInicio >= :horaAtual
              and a.status = :status
            order by a.horaInicio asc, a.id asc
            """)
    List<Agendamento> findProximosDoBarbeiroNoDia(
            @Param("barbeiroId") Long barbeiroId,
            @Param("data") LocalDate data,
            @Param("horaAtual") LocalTime horaAtual,
            @Param("status") StatusAgendamento status,
            Pageable pageable
    );

    Boolean existsByServico_Id(Long id);
    boolean existsByServicosPlano_ServicoId(Long id);

    Optional<Agendamento> findByIdAndCliente_Id(Long id, Long clienteId);


    List<Agendamento> findAllByBarbeiro_idAndData(Long barbeiro_id , LocalDate data);

    List<Agendamento> findAllByBarbeiro_Usuario_LoginAndDataOrderByHoraInicioAsc(String login, LocalDate data);

    boolean existsByPlanoMensalCliente_IdAndDataBetweenAndStatusNotIn(
            Long planoMensalClienteId,
            LocalDate inicioSemana,
            LocalDate fimSemana,
            Collection<StatusAgendamento> statusIgnorados
    );

    @Query("""
            select a
            from Agendamento a
            where a.cliente.id = :clienteId
              and a.status = :status
              and (
                    a.data > :dataAtual
                    or (a.data = :dataAtual and a.horaInicio >= :horaAtual)
                  )
            order by a.data asc, a.horaInicio asc
            """)
    List<Agendamento> findProximosDoCliente(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusAgendamento status,
            @Param("dataAtual") LocalDate dataAtual,
            @Param("horaAtual") LocalTime horaAtual,
            Pageable pageable
    );

    @Query("""
            select a
            from Agendamento a
            where a.cliente.id = :clienteId
              and (
                    a.status <> :statusAgendado
                    or a.data < :dataAtual
                    or (a.data = :dataAtual and a.horaInicio < :horaAtual)
                  )
            order by a.data desc, a.horaInicio desc
            """)
    List<Agendamento> findAntigosDoCliente(
            @Param("clienteId") Long clienteId,
            @Param("statusAgendado") StatusAgendamento statusAgendado,
            @Param("dataAtual") LocalDate dataAtual,
            @Param("horaAtual") LocalTime horaAtual
    );

    @Query("""
            select coalesce(sum(a.duracaoServicoMinutos), 0) from Agendamento a
            where a.barbeiro.id = :barbeiroId and a.data between :inicio and :fim
              and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            """)
    long somarMinutosConcluidos(@Param("barbeiroId") Long barbeiroId,
                               @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

}
