package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.ComissaoPlano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ComissaoPlanoRepository extends JpaRepository<ComissaoPlano, Long> {
    boolean existsByAgendamento_Id(Long agendamentoId);

    Optional<ComissaoPlano> findByAgendamento_Id(Long agendamentoId);

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(c.pontos), 0) from ComissaoPlano c join c.comanda f
            where f.barbeiro.id = :barbeiroId
              and cast(f.fechadoEm as date) between :inicio and :fim
            """)
    long somarPontosConcluidos(@org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
                              @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
                              @org.springframework.data.repository.query.Param("fim") LocalDate fim);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"comanda", "agendamento", "agendamento.cliente", "planoMensal"})
    @org.springframework.data.jpa.repository.Query("""
            select c from ComissaoPlano c join c.comanda f
            where f.barbeiro.id = :barbeiroId
              and cast(f.fechadoEm as date) between :inicio and :fim
            order by f.fechadoEm desc, c.id desc
            """)
    org.springframework.data.domain.Page<ComissaoPlano> extratoConcluidos(
            @org.springframework.data.repository.query.Param("barbeiroId") Long barbeiroId,
            @org.springframework.data.repository.query.Param("inicio") java.time.LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") java.time.LocalDate fim,
            org.springframework.data.domain.Pageable pageable);

}
