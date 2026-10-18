package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comissao_plano")
@Getter
@Setter
@NoArgsConstructor
public class ComissaoPlano {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private Agendamento agendamento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_mensal_id", nullable = false)
    private PlanosMensal planoMensal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_mensal_cliente_id", nullable = false)
    private PlanoMensalCliente planoMensalCliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(name = "data_agendamento", nullable = false)
    private LocalDate dataAgendamento;

    @Column(name = "pontos", nullable = false)
    private int pontos;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
