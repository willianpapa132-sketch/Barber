package bruninho.Barbeiro.Controller.Admin.requests;

import java.util.List;


public record ListJornadaSemanaBarbeiros(Long barbeiroid, List<CriarJornadasDosBarbeiros> jornadaBarbeiros) {
}
