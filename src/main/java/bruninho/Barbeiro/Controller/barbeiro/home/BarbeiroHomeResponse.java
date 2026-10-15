package bruninho.Barbeiro.Controller.barbeiro.home;

import java.time.LocalDate;
import java.util.List;

public record BarbeiroHomeResponse(
        BarbeiroResumo barbeiro,
        LocalDate data,
        long agendamentosHoje,
        long concluidosHoje,
        long canceladosHoje,
        List<ProximoAgendamentoBarbeiroResponse> proximosAgendamentos
) {
    public record BarbeiroResumo(Long id, String nome, String imgUrl) {}
}
