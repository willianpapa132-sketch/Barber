package bruninho.Barbeiro.domain;

import bruninho.Barbeiro.security.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plano_mensal_cliente")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanoMensalCliente {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    @Column(name = "valor_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "agendamentos_semana", nullable = false)
    private int agendamentosSemana;

    @Column(name = "agendamento_mes", nullable = false)
    private int agendamentosMes;

    @Column(name = "agendamentos_Indeterminados")
    private Boolean agendamentosIndeterminado;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo ;

    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @ManyToOne(optional = false)
    private PlanosMensal planoMensal;

    @Positive
    private Integer diasMaximoAntecedencia;


}
