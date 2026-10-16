package bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CriarBloqueioAgendaRequest(@NotNull LocalDate data, @NotNull LocalTime horaInicio,
                                        @NotNull LocalTime horaFim) {}
