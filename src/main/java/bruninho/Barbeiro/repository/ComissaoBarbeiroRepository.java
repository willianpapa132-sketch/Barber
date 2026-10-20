package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ComissaoBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComissaoBarbeiroRepository extends JpaRepository<ComissaoBarbeiro, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"barbeiro", "agendamento", "agendamento.cliente", "agendamento.servico"})
    @org.springframework.data.jpa.repository.Query("""
            select c from ComissaoBarbeiro c join c.agendamento a
            where a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            and a.pagamentoRecebido = true and c.valorComissao > 0
            and a.finalizadoEm >= :inicio and a.finalizadoEm < :fim
            order by a.finalizadoEm desc, c.id desc
            """)
    java.util.List<ComissaoBarbeiro> despesasFinanceiro(
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDateTime fim);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(c.valorComissao), 0) from ComissaoBarbeiro c join c.agendamento a where a.data = :data and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO")
    java.math.BigDecimal somarComissoesConcluidasNoDia(@org.springframework.data.repository.query.Param("data") java.time.LocalDate data);
    boolean existsByAgendamento_Id(Long agendamentoId);
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(c.valorComissao), 0) from ComissaoBarbeiro c join c.agendamento a
            where c.barbeiro.id = :barbeiroId and a.barbeiro.id = :barbeiroId
              and a.data between :inicio and :fim
              and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            """)
    java.math.BigDecimal somarComissoesConcluidas(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"agendamento", "agendamento.cliente", "agendamento.planoMensalCliente", "agendamento.planoMensalCliente.planoMensal"})
    @org.springframework.data.jpa.repository.Query("""
            select c from ComissaoBarbeiro c join c.agendamento a
            where c.barbeiro.id = :barbeiroId and a.barbeiro.id = :barbeiroId
              and a.data between :inicio and :fim
              and a.status = bruninho.Barbeiro.domain.StatusAgendamento.CONCLUIDO
            order by a.data desc, a.horaInicio desc, c.id desc
            """)
    org.springframework.data.domain.Page<ComissaoBarbeiro> extratoConcluidos(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim,
            org.springframework.data.domain.Pageable pageable);

}
