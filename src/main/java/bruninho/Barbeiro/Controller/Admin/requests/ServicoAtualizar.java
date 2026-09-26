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
public class ServicoAtualizar {

    @NotBlank
    private Long id;

    @NotBlank(message = "não pode ser salvo sem descrição do servico")
    private String nome;

    @Positive(message = "deve conter tempo acima minimo acima de 0")
    private int  tempoMinutos;


    private BigDecimal valor;

    private Boolean ativo;
}
