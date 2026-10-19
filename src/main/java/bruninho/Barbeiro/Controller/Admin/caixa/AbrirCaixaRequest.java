package bruninho.Barbeiro.Controller.Admin.caixa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record AbrirCaixaRequest(@NotNull @DecimalMin("0.00") BigDecimal saldoInicial) {}
