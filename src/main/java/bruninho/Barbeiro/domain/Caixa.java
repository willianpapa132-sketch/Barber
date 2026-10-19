package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Caixa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoInicial;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEntradas = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSangrias = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoFinal;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusCaixa status;
    @Column(nullable = false) private LocalDateTime abertoEm;
    private LocalDateTime fechadoEm;
}
