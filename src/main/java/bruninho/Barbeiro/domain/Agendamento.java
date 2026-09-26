package bruninho.Barbeiro.domain;

import bruninho.Barbeiro.security.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Agendamento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    @ManyToMany
    @JoinTable(
            name = "agendamento_servicos",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private Set<Servico> servico = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "plano_mensal_id")
    private PlanoMensalCliente planoMensal;

    @Column(name = "nome_cliente")
    private String nomeCliente;

    @Column(name = "telefone_cliente")
    private String telefoneCliente;

    @Column(name = "nome_servico_plano")
    private String nomeServicoPlano;

    @Column(name = "preco_total", precision = 12, scale = 2)
    private BigDecimal precoTotal;

    @Column(name = "duracao_servico_minutos")
    private int duracaoServicoMinutos;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    @Column(name = "pagamento_recebido")
    private boolean pagamentoRecebido;

    @ManyToOne
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPorUsuario;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm ;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm ;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

}
