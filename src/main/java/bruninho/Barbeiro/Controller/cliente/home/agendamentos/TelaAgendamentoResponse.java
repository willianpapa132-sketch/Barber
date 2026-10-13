package bruninho.Barbeiro.Controller.cliente.home.agendamentos;

import java.util.List;

public record TelaAgendamentoResponse(
        Long usuarioId,
        Long clienteId,
        boolean possuiPlano,
        String nomePlano,
        List<BarbeiroAgendamentoResponse> barbeiros,
        List<ServicoAgendamentoResponse> servicos
) {
}
