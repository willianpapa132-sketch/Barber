package bruninho.Barbeiro.Controller.cliente;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClienteWebController {

    @GetMapping("/plano")
    public String plano() {
        return "forward:/client/index.html";
    }

    @GetMapping("/agendamentos")
    public String agendamentos() {
        return "forward:/client/index.html";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "forward:/client/index.html";
    }

    @GetMapping("/agendamentos/novo")
    public String novoAgendamento() {
        return "forward:/client/index.html";
    }
}
