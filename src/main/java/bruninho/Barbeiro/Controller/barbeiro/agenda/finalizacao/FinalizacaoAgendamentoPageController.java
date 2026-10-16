package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FinalizacaoAgendamentoPageController {
    @GetMapping("/barbeiro/agendamentos/{agendamentoId}/finalizacao")
    public String pagina() {
        return "forward:/client/index.html";
    }
}
