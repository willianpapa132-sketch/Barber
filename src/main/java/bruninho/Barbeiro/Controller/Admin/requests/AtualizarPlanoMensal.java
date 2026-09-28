package bruninho.Barbeiro.Controller.Admin.requests;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
public class AtualizarPlanoMensal {


    private Long id;

    private String nomePlano;

    private Integer agendamentosMensal;

    private Integer agendamentosSemana;

    private BigDecimal valorMensal;

    private Set<Long> servicosId;



}
