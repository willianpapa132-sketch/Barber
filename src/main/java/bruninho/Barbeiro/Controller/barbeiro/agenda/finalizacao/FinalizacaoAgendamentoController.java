package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import bruninho.Barbeiro.service.AgendamentoStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbeiro/agendamentos/{agendamentoId}/finalizacao")
public class FinalizacaoAgendamentoController {
    private final AgendamentoStatusService finalizacao;

    public FinalizacaoAgendamentoController(AgendamentoStatusService finalizacao) {
        this.finalizacao = finalizacao;
    }

    @PostMapping("/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long agendamentoId, Authentication authentication) {
        finalizacao.cancelar(authentication.getName(), agendamentoId);
    }

    @PostMapping("/nao-compareceu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void naoCompareceu(@PathVariable Long agendamentoId, Authentication authentication) {
        finalizacao.registrarNaoComparecimento(authentication.getName(), agendamentoId);
    }

}
