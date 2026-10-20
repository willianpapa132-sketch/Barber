package bruninho.Barbeiro.Controller.Admin.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FinanceiroResponse(LocalDate inicio, LocalDate fim, BigDecimal valorBruto,
                                 BigDecimal despesas, BigDecimal valorLiquido,
                                 List<Lancamento> entradas, List<Lancamento> saidas) {
    public record Lancamento(Long id, LocalDateTime dataHora, String tipo, String descricao,
                             String cliente, String barbeiro, String formaPagamento, BigDecimal valor) {}
}
