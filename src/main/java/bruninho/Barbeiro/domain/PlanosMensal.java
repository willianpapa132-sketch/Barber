package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "plano_mensal")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanosMensal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomePlano;

    @Column(name = "valor_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo ;

    @ManyToMany
    @JoinTable(
            name = "plano_servicos",
            joinColumns = @JoinColumn(name = "plano_mensal_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private Set<Servico> servicosIncluidos = new HashSet<>();

    @Positive
    private Integer diasMaximoAntecedencia;


    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm ;
}
