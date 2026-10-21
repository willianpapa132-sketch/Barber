package bruninho.Barbeiro.Controller.Admin.deshboard;
import bruninho.Barbeiro.Controller.Admin.caixa.CaixaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record DashboardResponse(String periodo, LocalDate inicio, LocalDate fim, String agrupamento,
        long agendamentos, long concluidos, long cancelados, BigDecimal valorBruto, BigDecimal despesas,
        BigDecimal valorLiquido, BigDecimal entradasAgendamentos, BigDecimal entradasPlanos,
        List<Ponto> grafico, CaixaResponse caixa, List<Destaque> destaques, Planos planos) {
    public record Ponto(LocalDate data, long quantidade) {}
    public record Destaque(String tipo, Long barbeiroId, String nome, String imagem, BigDecimal valor, long atendimentos) {}
    public record Planos(long ativos, long pagosPeriodo, long pendentes, long vencendoHoje) {}
}
