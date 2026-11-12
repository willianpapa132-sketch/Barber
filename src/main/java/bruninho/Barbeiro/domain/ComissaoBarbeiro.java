package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@jakarta.persistence.EntityListeners(bruninho.Barbeiro.platform.EmpresaEntityListener.class)
@org.hibernate.annotations.Filter(name = "empresa", condition = "{e}.agendamento_id in (select p.id from agendamento p where p.empresa_id = :empresaId)", deduceAliasInjectionPoints = false, aliases = @org.hibernate.annotations.SqlFragmentAlias(alias = "e", table = "comissao_barbeiro"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComissaoBarbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
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