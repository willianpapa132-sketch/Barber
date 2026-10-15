package bruninho.Barbeiro.Controller.barbeiro.agenda;

import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.service.ConsultaAgendaBarbeiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/barbeiro/agenda")
public class AgendaBarbeiroController {
    private final ConsultaAgendaBarbeiroService agenda;

    public AgendaBarbeiroController(ConsultaAgendaBarbeiroService agenda) {
        this.agenda = agenda;
    }

    @GetMapping
    public AgendaBarbeiroResponse consultar(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return agenda.consultar(authentication.getName(), data, status, pagina, tamanho);
    }
}
