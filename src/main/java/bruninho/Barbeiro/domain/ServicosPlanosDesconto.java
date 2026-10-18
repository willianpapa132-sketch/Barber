package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servicos_planos_desconto")
@Getter
@Setter
@NoArgsConstructor
public class ServicosPlanosDesconto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Min(0) @Max(100)
    @Column(name = "porcentagem_desconto", nullable = false)
    private Integer porcentagemDesconto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_mensal_id", nullable = false)
    private PlanosMensal planoMensal;

    @ManyToMany
    @JoinTable(name = "servicos_planos_desconto_servicos",
            joinColumns = @JoinColumn(name = "desconto_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private Set<Servico> servicos = new HashSet<>();
}
