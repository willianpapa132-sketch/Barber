package bruninho.Barbeiro.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Embeddable @Getter @Setter @NoArgsConstructor
public class ItemComanda {
    @Column(nullable=false) private String tipo;
    private Long servicoId;
    private Long produtoId;
    @Column(nullable=false) private String nome;
    @Column(nullable=false) private int quantidade;
    @Column(nullable=false) private int minutos;
    @Column(nullable=false) private int pontos;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal precoUnitario;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal desconto;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal percentualComissao;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal comissao;
    @Column(nullable=false) private boolean incluidoNoPlano;
}
