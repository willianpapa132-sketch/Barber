package bruninho.Barbeiro.Controller.cliente.request;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CadastrarPlano {


    private Long clienteid;

    private Long planoMensalid;
}
