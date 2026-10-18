package bruninho.Barbeiro.Controller.cliente.home.agendamentos;

import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.service.CalculoServicosPlano;
import java.math.BigDecimal;

public record ServicoAgendamentoResponse(Long id, String nome, BigDecimal preco, Integer duracaoMinutos,
                                        boolean inclusoNoPlano, int porcentagemDesconto, BigDecimal precoLiquido) {
    public static ServicoAgendamentoResponse de(Servico servico, PlanosMensal plano) {
        var item = CalculoServicosPlano.calcular(servico, plano);
        return new ServicoAgendamentoResponse(servico.getId(), servico.getNome(), item.getPrecoOriginal(),
                servico.getDuracaoMinutos(), item.isInclusoNoPlano(), item.getPorcentagemDesconto(), item.getPrecoLiquido());
    }
}
