package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "movimento_caixa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MovimentoCaixa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "caixa_id")
    private Caixa caixa;
    @ManyToOne @JoinColumn(name = "agendamento_id", unique = true)
    private Agendamento agendamento;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoMovimentoCaixa tipo;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private LocalDateTime criadoEm;
    private String observacao;
}
