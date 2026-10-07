package bruninho.Barbeiro.Controller.cliente.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CriarAgendamentoRequest(
        @NotNull @Positive Long usuarioId,
        @NotNull @Positive Long clienteId,
        @NotNull @Positive Long barbeiroId,
        @NotEmpty List<@NotNull @Positive Long> servicosIds,
        @NotNull LocalDate data,
        @NotNull LocalTime horaInicio) {
}
