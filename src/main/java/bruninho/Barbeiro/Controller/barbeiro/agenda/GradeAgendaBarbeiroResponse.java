package bruninho.Barbeiro.Controller.barbeiro.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GradeAgendaBarbeiroResponse(
        LocalDate data, LocalTime inicio, LocalTime fim, Integer intervaloGrade,
        List<LocalTime> horarios, List<AgendamentoAgendaBarbeiroResponse> agendamentos,
        List<Periodo> indisponibilidades, boolean podeCriar, String aviso
) {
    public record Periodo(Long id, String tipo, String descricao, LocalTime inicio, LocalTime fim) {}
}
