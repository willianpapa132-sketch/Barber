package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;


import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Set;


@Getter
@Setter
public class CriarPlanoMensal {

    @NotBlank
    private String nomePlano;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 2 casas decimais")
    private BigDecimal valorMensal ;

    private boolean ativo = true;

    private Set<Long> servicosId;

    @jakarta.validation.Valid
    private java.util.List<DescontoServicoPlanoRequest> descontosServicos;

    @NotBlank
    private String descricao;

    private Integer diasMaximoAntecedencia;




}
