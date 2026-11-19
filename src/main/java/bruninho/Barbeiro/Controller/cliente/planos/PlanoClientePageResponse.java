package bruninho.Barbeiro.Controller.cliente.planos;

import java.util.List;

public record PlanoClientePageResponse(
        boolean possuiPlano,
        PlanoContratadoResponse planoAtual,
        List<PlanoMensalResponse> planosDisponiveis,
        List<bruninho.Barbeiro.Controller.cliente.home.agendamentos.BarbeiroAgendamentoResponse> barbeiros,
        bruninho.Barbeiro.service.CancelamentoPlanoClienteService.Situacao cancelamento
) {
}
