package bruninho.Barbeiro.Controller.barbeiro;

import bruninho.Barbeiro.service.AgendaBarbeiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/barbeiro")
public class BarbeiroWebController {

    private final AgendaBarbeiroService agendaBarbeiroService;

    public BarbeiroWebController(AgendaBarbeiroService agendaBarbeiroService) {
        this.agendaBarbeiroService = agendaBarbeiroService;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("login", authentication.getName());
        return "barbeiro/home";
    }

    @GetMapping("/agenda")
    public String agendaHoje(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model
    ) {
        LocalDate hoje = LocalDate.now();
        LocalDate dataSelecionada = agendaBarbeiroService.ajustarDataSelecionada(data, hoje);

        model.addAttribute("data", dataSelecionada);
        model.addAttribute("hoje", hoje);
        model.addAttribute("dataMaxima", agendaBarbeiroService.dataMaximaAgenda(hoje));
        model.addAttribute("datasPermitidas", agendaBarbeiroService.datasPermitidas(hoje));
        model.addAttribute("dataAjustada", agendaBarbeiroService.dataFoiAjustada(data, dataSelecionada));
        model.addAttribute("agendamentos", agendaBarbeiroService.agendaDoDia(authentication.getName(), dataSelecionada));

        return "barbeiro/agenda";
    }
}
