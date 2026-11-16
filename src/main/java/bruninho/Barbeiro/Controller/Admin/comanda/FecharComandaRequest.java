package bruninho.Barbeiro.Controller.Admin.comanda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import bruninho.Barbeiro.domain.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
public record FecharComandaRequest(
    @NotEmpty @Size(max=100) List<@NotNull @Valid Item> itens,
    @NotNull TipoDesconto tipoDesconto,
    @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal desconto,
    @Size(max=500) String observacao,
    @NotNull @Size(max=10) List<@NotNull @Valid Pagamento> pagamentos
) {
    public enum TipoDesconto { NENHUM, VALOR, PERCENTUAL }
    public enum TipoItem { SERVICO, PRODUTO }
    public record Item(@NotNull TipoItem tipo,@NotNull @Positive Long id,@Min(1) @Max(999) int quantidade) {}
    public record Pagamento(@NotNull FormaPagamento forma,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal valor) {}
}
