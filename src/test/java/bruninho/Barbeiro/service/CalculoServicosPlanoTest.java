package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class CalculoServicosPlanoTest {
    private Servico servico(Long id, String preco, int minutos) {
        var s = new Servico(); s.setId(id); s.setNome("Servico " + id);
        s.setPreco(new BigDecimal(preco)); s.setDuracaoMinutos(minutos); return s;
    }

    private void desconto(PlanosMensal plano, int percentual, Servico servico) {
        var regra = new ServicosPlanosDesconto(); regra.setPlanoMensal(plano);
        regra.setPorcentagemDesconto(percentual); regra.setServicos(Set.of(servico));
        plano.getDescontosServicos().add(regra);
    }

    @Test
    void incluidoTemPrioridadeEDescontoNaoGeraPontosParaExtras() {
        var corte = servico(1L, "50", 30); var extra = servico(2L, "35", 15);
        var normal = servico(3L, "10", 5); var plano = new PlanosMensal();
        plano.setServicosIncluidos(Set.of(corte)); desconto(plano, 20, corte); desconto(plano, 20, extra);
        var itens = CalculoServicosPlano.calcular(Set.of(corte, extra, normal), plano);
        assertThat(CalculoServicosPlano.total(itens)).isEqualByComparingTo("38.00");
        assertThat(CalculoServicosPlano.pontos(itens)).isEqualTo(30);
        assertThat(CalculoServicosPlano.calcular(corte, plano).getPrecoLiquido()).isZero();
        assertThat(CalculoServicosPlano.calcular(normal, plano).getPrecoLiquido()).isEqualByComparingTo("10");
    }

    @Test
    void arredondaPorServicoEAceitaDescontoIntegralSemPontos() {
        var extra = servico(1L, "19.99", 10); var plano = new PlanosMensal();
        desconto(plano, 15, extra);
        assertThat(CalculoServicosPlano.calcular(extra, plano).getPrecoLiquido()).isEqualByComparingTo("16.99");
        plano.getDescontosServicos().iterator().next().setPorcentagemDesconto(100);
        var item = CalculoServicosPlano.calcular(extra, plano);
        assertThat(item.getPrecoLiquido()).isZero();
        assertThat(item.isInclusoNoPlano()).isFalse();
    }

    @Test
    void rejeitaPercentuaisInvalidosERegrasSobrepostas() {
        var extra = servico(1L, "20", 10); var plano = new PlanosMensal();
        desconto(plano, 101, extra);
        assertThatThrownBy(() -> CalculoServicosPlano.calcular(extra, plano)).hasMessageContaining("entre 0 e 100");
        plano.getDescontosServicos().iterator().next().setPorcentagemDesconto(-1);
        assertThatThrownBy(() -> CalculoServicosPlano.calcular(extra, plano)).hasMessageContaining("entre 0 e 100");
        plano.getDescontosServicos().iterator().next().setPorcentagemDesconto(10);
        desconto(plano, 20, extra);
        assertThatThrownBy(() -> CalculoServicosPlano.calcular(extra, plano)).hasMessageContaining("mais de uma regra");
    }
}
