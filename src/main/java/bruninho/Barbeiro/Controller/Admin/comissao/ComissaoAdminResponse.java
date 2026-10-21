package bruninho.Barbeiro.Controller.Admin.comissao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record ComissaoAdminResponse(LocalDate inicio, LocalDate fim, LocalDate hoje, Long barbeiroId,
        long minutosTrabalhados, long pontosPlano, BigDecimal valorComissoes, BigDecimal comissoesHoje,
        List<Resumo> barbeiros) {
    public record Resumo(Long id, String nome, boolean ativo, long minutosTrabalhados,
                         long pontosPlano, BigDecimal valorComissoes) {}
}
