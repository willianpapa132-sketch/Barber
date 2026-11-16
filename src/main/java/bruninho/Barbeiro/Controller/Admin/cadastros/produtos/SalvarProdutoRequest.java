package bruninho.Barbeiro.Controller.Admin.cadastros.produtos;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record SalvarProdutoRequest(
    @NotBlank @Size(max=255) String nome,
    @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal preco,
    @NotNull @Positive Integer periodicidadeMeses,
    @NotNull @PositiveOrZero Integer quantidadeEstoque,
    @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal percentualComissao
) {
    public SalvarProdutoRequest(String nome,BigDecimal preco,Integer periodicidadeMeses,Integer quantidadeEstoque){this(nome,preco,periodicidadeMeses,quantidadeEstoque,null);}
}
