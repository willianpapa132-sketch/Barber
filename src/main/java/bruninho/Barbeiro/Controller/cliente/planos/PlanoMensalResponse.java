package bruninho.Barbeiro.Controller.cliente.planos;

import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.domain.Servico;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record PlanoMensalResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valorMensal,
        Integer diasMaximoAntecedencia,
        List<ServicoPlanoResponse> servicos
) {
    public static PlanoMensalResponse de(PlanosMensal plano) {
        return new PlanoMensalResponse(
                plano.getId(),
                plano.getNomePlano(),
                plano.getDescricao(),
                plano.getValorMensal(),
                plano.getDiasMaximoAntecedencia(),
                plano.getServicosIncluidos().stream()
                        .sorted(Comparator.comparing(Servico::getNome))
                        .map(ServicoPlanoResponse::de)
                        .toList()
        );
    }
}
