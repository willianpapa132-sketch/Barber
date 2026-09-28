package bruninho.Barbeiro.Controller.cliente.request;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
public class CadastrarPlano {


    private Long clienteid;


    private Long barbeiroid;


    private Long planoMensalid;
}
