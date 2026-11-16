package bruninho.Barbeiro.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PagamentoComanda {
    @Enumerated(EnumType.STRING) @Column(nullable=false) private FormaPagamento forma;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal valor;
}
