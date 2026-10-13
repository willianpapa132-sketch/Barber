package bruninho.Barbeiro.Controller.cliente.agendamentos;

import java.util.List;

public record AgendamentosClientePageResponse(
        List<AgendamentoClienteResumoResponse> proximos,
        List<AgendamentoClienteResumoResponse> antigos
) {
}
