package bruninho.Barbeiro.Controller.cliente.agendamentos;

import bruninho.Barbeiro.service.AgendamentosClienteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/agendamentos")
public class AgendamentosClienteController {
    private final AgendamentosClienteService agendamentos;

    public AgendamentosClienteController(AgendamentosClienteService agendamentos) {
        this.agendamentos = agendamentos;
    }

    @GetMapping
    public AgendamentosClientePageResponse listar(Authentication authentication) {
        return agendamentos.listar(authentication.getName());
    }

    @GetMapping("/{agendamentoId}")
    public AgendamentoClienteItemResponse detalhe(
            @PathVariable Long agendamentoId,
            Authentication authentication
    ) {
        return agendamentos.detalhe(authentication.getName(), agendamentoId);
    }
}
