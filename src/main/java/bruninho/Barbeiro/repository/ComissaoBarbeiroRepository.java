package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ComissaoBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComissaoBarbeiroRepository extends JpaRepository<ComissaoBarbeiro, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(c.valorComissao), 0) from ComissaoBarbeiro c join c.comanda f where cast(f.fechadoEm as date) = :data")
    java.math.BigDecimal somarComissoesConcluidasNoDia(@org.springframework.data.repository.query.Param("data") java.time.LocalDate data);
    boolean existsByAgendamento_Id(Long agendamentoId);
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(c.valorComissao), 0) from ComissaoBarbeiro c join c.comanda f
            where f.barbeiro.id = :barbeiroId
              and cast(f.fechadoEm as date) between :inicio and :fim
            """)
    java.math.BigDecimal somarComissoesConcluidas(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"comanda", "agendamento", "agendamento.cliente", "agendamento.planoMensalCliente", "agendamento.planoMensalCliente.planoMensal"})
    @org.springframework.data.jpa.repository.Query("""
            select c from ComissaoBarbeiro c join c.comanda f
            where f.barbeiro.id = :barbeiroId
              and cast(f.fechadoEm as date) between :inicio and :fim
            order by f.fechadoEm desc, c.id desc
            """)
    org.springframework.data.domain.Page<ComissaoBarbeiro> extratoConcluidos(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim,
            org.springframework.data.domain.Pageable pageable);

}
