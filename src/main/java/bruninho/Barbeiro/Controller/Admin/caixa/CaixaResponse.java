package bruninho.Barbeiro.Controller.Admin.caixa;
import bruninho.Barbeiro.domain.Caixa;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record CaixaResponse(Long id, BigDecimal saldoInicial, BigDecimal totalEntradas, BigDecimal totalSangrias,
                            BigDecimal saldoAtual, BigDecimal saldoFinal, String status,
                            LocalDateTime abertoEm, LocalDateTime fechadoEm) {
    public static CaixaResponse de(Caixa c) {
        var atual = c.getSaldoInicial().add(c.getTotalEntradas()).subtract(c.getTotalSangrias());
        return new CaixaResponse(c.getId(), c.getSaldoInicial(), c.getTotalEntradas(), c.getTotalSangrias(), atual,
                c.getSaldoFinal(), c.getStatus().name(), c.getAbertoEm(), c.getFechadoEm());
    }
}
