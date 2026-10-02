package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "configuracao_barbearia")
@Getter
@Setter
public class ConfiguracaoBarbearia {

    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private String nome = "Barbearia Bruninho";
    private String telefone;
    private String endereco;
    @PositiveOrZero
    private Integer diasMaximoAntecedentia;


}
