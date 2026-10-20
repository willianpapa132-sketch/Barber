package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.RecebimentoPlano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.time.LocalDate;
import java.util.List;

public interface RecebimentoPlanoRepository extends JpaRepository<RecebimentoPlano, Long> {
    List<RecebimentoPlano> findByContrato_IdOrderByVencimentoDesc(Long contratoId);
    @EntityGraph(attributePaths = {"contrato", "contrato.cliente", "contrato.planoMensal"})
    List<RecebimentoPlano> findByDataPagamentoBetweenOrderByDataPagamentoDescIdDesc(LocalDate inicio, LocalDate fim);
}
