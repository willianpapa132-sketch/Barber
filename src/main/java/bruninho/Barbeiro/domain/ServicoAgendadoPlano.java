package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

/** Condicoes do servico aceitas no momento do agendamento com plano. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ServicoAgendadoPlano {
    @Column(name = "servico_id", nullable = false)
    private Long servicoId;
    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;
    @Column(name = "incluso_no_plano", nullable = false)
    private boolean inclusoNoPlano;
    @Column(name = "preco_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoOriginal;
    @Column(name = "porcentagem_desconto", nullable = false)
    private int porcentagemDesconto;
    @Column(name = "preco_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoLiquido;
}
