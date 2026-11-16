package bruninho.Barbeiro.Controller.Admin.cadastros.produtos;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record SalvarProdutoRequest(
    @NotBlank @Size(max=255) String nome,
    @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal preco,
    @NotNull @Positive Integer periodicidadeMeses,
    @NotNull @PositiveOrZero Integer quantidadeEstoque
) {}
