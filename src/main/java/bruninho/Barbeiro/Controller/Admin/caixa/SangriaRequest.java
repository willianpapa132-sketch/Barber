package bruninho.Barbeiro.Controller.Admin.caixa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record SangriaRequest(@NotNull @DecimalMin(value="0.01") BigDecimal valor, String observacao) {}
