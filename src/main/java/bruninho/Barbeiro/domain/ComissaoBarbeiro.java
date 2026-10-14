package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComissaoBarbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Agendamento agendamento;

    @ManyToOne
    private Barbeiro barbeiro;

    @Enumerated(EnumType.STRING)
    private TipoComissao tipo;

    private BigDecimal valorBase;

    private BigDecimal percentualAplicado;

    private BigDecimal valorComissao;

    private LocalDateTime criadoEm;
}