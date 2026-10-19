package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
public class AtualizarPlanoMensal {


    private Long id;

    private String nomePlano;

    private BigDecimal valorMensal;

    private Integer diasMaximoAntecedencia;

    private Set<Long> servicosId;

    @jakarta.validation.Valid
    private java.util.List<DescontoServicoPlanoRequest> descontosServicos;

    @NotBlank
    private String descricao;

}
