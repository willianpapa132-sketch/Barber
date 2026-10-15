package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.service.FinalizacaoAgendamentoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbeiro/agendamentos/{agendamentoId}/finalizacao")
public class FinalizacaoAgendamentoController {
    private final FinalizacaoAgendamentoService finalizacao;

    public FinalizacaoAgendamentoController(FinalizacaoAgendamentoService finalizacao) {
        this.finalizacao = finalizacao;
    }

    @GetMapping
    public FinalizacaoAgendamentoResponse consultar(
            @PathVariable Long agendamentoId,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            Authentication authentication
    ) {
        return finalizacao.consultar(authentication.getName(), agendamentoId, formaPagamento);
    }

    @PostMapping
    public FinalizacaoAgendamentoResponse finalizar(
            @PathVariable Long agendamentoId,
            @Valid @RequestBody FinalizarAgendamentoRequest request,
            Authentication authentication
    ) {
        return finalizacao.finalizar(authentication.getName(), agendamentoId, request.formaPagamento());
    }
}
