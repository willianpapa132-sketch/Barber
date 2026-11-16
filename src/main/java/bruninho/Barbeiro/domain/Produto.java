package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Filter(name = "empresa", condition = "{e}.empresa_id = :empresaId", deduceAliasInjectionPoints = false, aliases = @org.hibernate.annotations.SqlFragmentAlias(alias = "e", table = "produto"))
@Table(name = "produto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Produto extends bruninho.Barbeiro.platform.EmpresaScopedEntity {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer periodicidadeMeses;

    @Column(nullable = false)
    private Integer quantidadeEstoque;

    @Column(nullable=false,precision=5,scale=2)
    private BigDecimal percentualComissao = BigDecimal.ZERO;

    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime atualizadoEm = LocalDateTime.now();


}
