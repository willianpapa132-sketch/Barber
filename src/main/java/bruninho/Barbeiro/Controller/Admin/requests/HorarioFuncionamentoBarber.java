package bruninho.Barbeiro.Controller.Admin.requests;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioFuncionamentoBarber(DayOfWeek diaSemana, LocalTime horarioInicio, LocalTime horarioFim, Boolean fechado) {
}
