package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class CalculoServicosPlano {
    private CalculoServicosPlano() {}

    public static ServicoAgendadoPlano calcular(Servico servico, PlanosMensal plano) {
        var item = new ServicoAgendadoPlano();
        item.setServicoId(servico.getId());
        item.setNome(servico.getNome());
        item.setDuracaoMinutos(servico.getDuracaoMinutos() == null ? 0 : servico.getDuracaoMinutos());
        if (servico.getPreco() == null || servico.getPreco().signum() < 0) {
            throw erro("Preco do servico invalido");
        }
        item.setPrecoOriginal(servico.getPreco().setScale(2, RoundingMode.HALF_UP));
        boolean incluido = plano != null && plano.getServicosIncluidos().stream()
                .anyMatch(s -> s.getId().equals(servico.getId()));
        item.setInclusoNoPlano(incluido);
        int desconto = 0;
        if (plano != null && !incluido) {
            var regras = plano.getDescontosServicos().stream()
                    .filter(r -> r.getServicos().stream().anyMatch(s -> s.getId().equals(servico.getId())))
                    .toList();
            if (regras.size() > 1) throw erro("Servico com mais de uma regra de desconto no plano");
            if (!regras.isEmpty()) {
                Integer percentual = regras.getFirst().getPorcentagemDesconto();
                if (percentual == null || percentual < 0 || percentual > 100) {
                    throw erro("Desconto do plano deve estar entre 0 e 100");
                }
                desconto = percentual;
            }
        }
        item.setPorcentagemDesconto(desconto);
        item.setPrecoLiquido(incluido ? BigDecimal.ZERO.setScale(2) : item.getPrecoOriginal()
                .multiply(BigDecimal.valueOf(100 - desconto)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return item;
    }

    public static List<ServicoAgendadoPlano> calcular(Set<Servico> servicos, PlanosMensal plano) {
        return servicos.stream().sorted(Comparator.comparing(Servico::getNome).thenComparing(Servico::getId))
                .map(s -> calcular(s, plano)).toList();
    }

    public static BigDecimal total(List<ServicoAgendadoPlano> itens) {
        return itens.stream().map(ServicoAgendadoPlano::getPrecoLiquido).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public static int pontos(List<ServicoAgendadoPlano> itens) {
        return itens.stream().filter(ServicoAgendadoPlano::isInclusoNoPlano)
                .mapToInt(ServicoAgendadoPlano::getDuracaoMinutos).sum();
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
