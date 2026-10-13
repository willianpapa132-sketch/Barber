package bruninho.Barbeiro.Controller.cliente.planos;

import bruninho.Barbeiro.domain.Servico;

import java.math.BigDecimal;

public record ServicoPlanoResponse(
        Long id,
        String nome,
        BigDecimal preco,
        Integer duracaoMinutos
) {
    public static ServicoPlanoResponse de(Servico servico) {
        return new ServicoPlanoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getPreco(),
                servico.getDuracaoMinutos()
        );
    }
}
