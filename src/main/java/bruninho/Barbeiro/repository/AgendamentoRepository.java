package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento,Long> {

    Boolean existsByServico_Id(Long id);


    List<Agendamento> findAllByBarbeiro_idAndData(Long barbeiro_id , LocalDate data);

    List<Agendamento> findAllByBarbeiro_Usuario_LoginAndDataOrderByHoraInicioAsc(String login, LocalDate data);

    boolean existsByPlanoMensalCliente_IdAndDataBetweenAndStatusNotIn(
            Long planoMensalClienteId,
            LocalDate inicioSemana,
            LocalDate fimSemana,
            Collection<StatusAgendamento> statusIgnorados
    );
}
