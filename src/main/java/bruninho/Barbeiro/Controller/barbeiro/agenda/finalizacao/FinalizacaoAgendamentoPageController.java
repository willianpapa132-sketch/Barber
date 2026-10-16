package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FinalizacaoAgendamentoPageController {
    @GetMapping("/barbeiro/agendamentos/{agendamentoId}/finalizacao")
    public String pagina(@PathVariable Long agendamentoId, Model model) {
        model.addAttribute("apiUrl", "/api/barbeiro/agendamentos/" + agendamentoId + "/finalizacao");
        return "barbeiro/finalizacao";
    }
}
