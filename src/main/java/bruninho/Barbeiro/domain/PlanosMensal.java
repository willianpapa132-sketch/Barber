package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.lang.model.element.Name;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "configuracao_plano_mensal")
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
    private BigDecimal valorMensal = BigDecimal.ZERO;

    @Column(name = "agendamentos_mes")
    private int agendamento_mes = 4;

    @Column(name = "agendamentos_semana")
    private int cortesPorSemana = 1;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    private Boolean prazoIndeterminado;

    @ManyToMany
    @JoinTable(
            name = "plano_servicos",
            joinColumns = @JoinColumn(name = "plano_mensal_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicosIncluidos;

    private LocalDateTime atualizadoEm = LocalDateTime.now();

}
