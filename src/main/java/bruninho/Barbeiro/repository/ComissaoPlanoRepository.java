package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ComissaoPlano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ComissaoPlanoRepository extends JpaRepository<ComissaoPlano, Long> {
    boolean existsByAgendamento_Id(Long agendamentoId);

    Optional<ComissaoPlano> findByAgendamento_Id(Long agendamentoId);

    long countByPlanoMensal_IdAndDataAgendamentoBetween(Long planoId, LocalDate inicio, LocalDate fim);

    long countByPlanoMensal_IdAndBarbeiro_IdAndDataAgendamentoBetween(
            Long planoId, Long barbeiroId, LocalDate inicio, LocalDate fim);
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(c.pontos), 0) from ComissaoPlano c join c.agendamento a
            where c.barbeiro.id = :barbeiroId and a.barbeiro.id = :barbeiroId
              and a.data between :inicio and :fim
              and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            """)
    long somarPontosConcluidos(@org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
                              @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
                              @org.springframework.data.repository.query.Param("fim") LocalDate fim);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"agendamento", "agendamento.cliente", "planoMensal"})
    @org.springframework.data.jpa.repository.Query("""
            select c from ComissaoPlano c join c.agendamento a
            where c.barbeiro.id = :barbeiroId and a.barbeiro.id = :barbeiroId
              and a.data between :inicio and :fim
              and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            order by a.data desc, a.horaInicio desc, c.id desc
            """)
    org.springframework.data.domain.Page<ComissaoPlano> extratoConcluidos(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim,
            org.springframework.data.domain.Pageable pageable);

}
