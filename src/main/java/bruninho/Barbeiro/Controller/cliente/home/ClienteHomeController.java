package bruninho.Barbeiro.Controller.cliente.home;

import bruninho.Barbeiro.service.HomeClienteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/home")
public class ClienteHomeController {
    private final HomeClienteService homeClienteService;

    public ClienteHomeController(HomeClienteService clienteHomeService) {
        this.homeClienteService = clienteHomeService;
    }

    @GetMapping
    public HomeResponseC home(Authentication authentication) {
        return homeClienteService.homeDoCliente(authentication.getName());
    }

    @GetMapping("/agendamentos/{agendamentoId}")
    public AgendamentoHomeResponse detalheAgendamento(
            @PathVariable Long agendamentoId,
            Authentication authentication
    ) {
        return homeClienteService.detalheAgendamento(authentication.getName(), agendamentoId);
    }
}
