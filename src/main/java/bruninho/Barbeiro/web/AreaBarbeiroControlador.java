package bruninho.Barbeiro.web;

import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.service.RegraNegocioException;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/barbeiro")
public class AreaBarbeiroControlador {
    private final BarbeiroRepositorio barbeiros;
    private final AgendamentoRepositorio agendamentos;

    public AreaBarbeiroControlador(BarbeiroRepositorio barbeiros, AgendamentoRepositorio agendamentos) {
        this.barbeiros = barbeiros;
        this.agendamentos = agendamentos;
    }

    @GetMapping("/agenda")
    String agenda(@RequestParam(required = false) LocalDate data, Authentication auth, Model model) {
        var barbeiro = barbeiros.findByUsuarioLogin(auth.getName()).orElseThrow(() -> new RegraNegocioException("Barbeiro não encontrado."));
        LocalDate dataAgenda = data == null ? LocalDate.now() : data;
        model.addAttribute("barbeiro", barbeiro);
        model.addAttribute("data", dataAgenda);
        model.addAttribute("agendamentos", agendamentos.findAgenda(barbeiro.getId(), dataAgenda.atStartOfDay(), dataAgenda.plusDays(1).atStartOfDay()));
        return "barbeiro/agenda";
    }
}
