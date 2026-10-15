package bruninho.Barbeiro.Controller.barbeiro.agenda;

import bruninho.Barbeiro.domain.StatusAgendamento;

import java.time.LocalDate;
import java.util.List;

public record AgendaBarbeiroResponse(
        LocalDate data,
        StatusAgendamento status,
        Long proximoAgendamentoId,
        List<AgendamentoAgendaBarbeiroResponse> agendamentos,
        int pagina,
        int tamanho,
        long totalAgendamentos,
        int totalPaginas,
        boolean temProximaPagina
) {}
