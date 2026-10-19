package bruninho.Barbeiro.Controller.Admin.cadastros.barbeiro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record SalvarBarbeiroRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 30) String telefone,
        @NotBlank @Email @Size(max = 254) String login,
        @Size(max = 72) String senha,
        @NotNull @Min(0) @Max(100) Integer comissao,
        @NotNull Boolean ativo,
        @Size(max = 2800000) String imgUrl,
        @NotNull @Size(min = 7, max = 7) List<@NotNull @Valid Dia> jornada
) {
    public record Dia(@NotNull DayOfWeek diaSemana, @NotNull Boolean folga,
                      LocalTime horaInicio, LocalTime horaFim,
                      LocalTime intervaloInicio, LocalTime intervaloFim) {}
}
