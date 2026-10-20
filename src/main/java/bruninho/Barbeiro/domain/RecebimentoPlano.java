package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "recebimento_plano", uniqueConstraints = @UniqueConstraint(columnNames = {"contrato_id", "vencimento"}))
public class RecebimentoPlano {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "contrato_id", nullable = false)
    private PlanoMensalCliente contrato;
    @Column(nullable = false) private LocalDate vencimento;
    @Column(nullable = false) private LocalDate dataPagamento;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FormaPagamento formaPagamento;
    @Column(nullable = false) private LocalDateTime registradoEm;
    @Column(nullable = false) private String registradoPor;
    @Column(length = 250) private String referencia;
}
