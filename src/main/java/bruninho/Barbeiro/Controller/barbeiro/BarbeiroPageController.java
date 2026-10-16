package bruninho.Barbeiro.Controller.barbeiro;

import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.service.ConsultaAgendaBarbeiroService;
import bruninho.Barbeiro.service.HomeBarbeiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/barbeiro")
public class BarbeiroPageController {
    private final HomeBarbeiroService home;
    private final ConsultaAgendaBarbeiroService agenda;

    public BarbeiroPageController(HomeBarbeiroService home, ConsultaAgendaBarbeiroService agenda) {
        this.home = home;
        this.agenda = agenda;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        var dados = home.homeDoBarbeiro(authentication.getName());
        model.addAttribute("home", dados);
        model.addAttribute("dataFormatada", dados.data().format(DateTimeFormatter.ofPattern(
                "d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))));
        return "barbeiro/home";
    }

    @GetMapping("/agenda")
    public String agenda(Authentication authentication, Model model,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         @RequestParam(required = false) StatusAgendamento status,
                         @RequestParam(defaultValue = "0") int pagina) {
        model.addAttribute("agenda", agenda.consultar(authentication.getName(), data, status, pagina, 20));
        return "barbeiro/agenda";
    }
}
