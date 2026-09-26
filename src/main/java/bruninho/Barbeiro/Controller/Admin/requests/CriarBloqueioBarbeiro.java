package bruninho.Barbeiro.Controller.Admin.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CriarBloqueioBarbeiro {


    @NotBlank
    private Long barbeiro_id;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataBloqueio;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFim;




}
