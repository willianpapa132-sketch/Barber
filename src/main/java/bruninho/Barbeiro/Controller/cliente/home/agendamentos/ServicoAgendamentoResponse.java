package bruninho.Barbeiro.Controller.cliente.home.agendamentos;

import bruninho.Barbeiro.domain.Servico;

import java.math.BigDecimal;

public record ServicoAgendamentoResponse(
        Long id,
        String nome,
        BigDecimal preco,
        Integer duracaoMinutos,
        boolean inclusoNoPlano
) {
    public static ServicoAgendamentoResponse de(Servico servico, boolean inclusoNoPlano) {
        return new ServicoAgendamentoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getPreco(),
                servico.getDuracaoMinutos(),
                inclusoNoPlano
        );
    }
}
