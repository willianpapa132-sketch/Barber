package bruninho.Barbeiro.Controller.barbeiro.agenda;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CriarAtendimentoBarbeiroRequest(
        @Positive Long clienteId,
        @Size(max = 120) String nomeCliente,
        @Size(max = 20) String telefoneCliente,
        @NotEmpty List<@NotNull @Positive Long> servicosIds,
        @NotNull LocalDate data,
        @NotNull LocalTime horaInicio
) {}
