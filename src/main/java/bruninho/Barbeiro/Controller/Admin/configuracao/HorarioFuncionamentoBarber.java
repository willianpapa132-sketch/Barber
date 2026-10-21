package bruninho.Barbeiro.Controller.Admin.configuracao;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
public record HorarioFuncionamentoBarber(@NotNull DayOfWeek diaSemana, LocalTime horarioInicio,
        LocalTime horarioFim, @NotNull Boolean fechado,
        @NotNull @Min(1) @Max(1440) Integer intervaloMinimoEntreAgendamentos,
        @NotNull Boolean permiteAgendamentoPlano) {}
