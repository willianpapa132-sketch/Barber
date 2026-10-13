package bruninho.Barbeiro.Controller.cliente.planos;

import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.Servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record PlanoContratadoResponse(
        Long id,
        Long planoMensalId,
        String nome,
        String descricao,
        BigDecimal valorMensal,
        Integer diasMaximoAntecedencia,
        LocalDateTime contratadoEm,
        List<ServicoPlanoResponse> servicos
) {
    public static PlanoContratadoResponse de(PlanoMensalCliente planoCliente) {
        var plano = planoCliente.getPlanoMensal();

        return new PlanoContratadoResponse(
                planoCliente.getId(),
                plano.getId(),
                plano.getNomePlano(),
                plano.getDescricao(),
                planoCliente.getValorMensal(),
                planoCliente.getDiasMaximoAntecedencia(),
                planoCliente.getCriadoEm(),
                plano.getServicosIncluidos().stream()
                        .sorted(Comparator.comparing(Servico::getNome))
                        .map(ServicoPlanoResponse::de)
                        .toList()
        );
    }
}
