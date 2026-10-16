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
}
