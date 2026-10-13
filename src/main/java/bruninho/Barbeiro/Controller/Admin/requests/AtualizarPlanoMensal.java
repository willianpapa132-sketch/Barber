package bruninho.Barbeiro.Controller.Admin.requests;


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

    @NotBlank
    private String descricao;

}
