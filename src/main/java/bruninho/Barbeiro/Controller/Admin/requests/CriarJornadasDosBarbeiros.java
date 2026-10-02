package bruninho.Barbeiro.Controller.Admin.requests;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class CriarJornadasDosBarbeiros {

    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private  LocalTime inicioIntervalo;
    private  LocalTime fimIntervalo;

}
