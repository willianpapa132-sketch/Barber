package bruninho.Barbeiro.Controller.Admin.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class CriacaoServico {

    @NotBlank
    private String nome;

    @Positive
    private int duracaoMinutos;


    private BigDecimal valor;
}
