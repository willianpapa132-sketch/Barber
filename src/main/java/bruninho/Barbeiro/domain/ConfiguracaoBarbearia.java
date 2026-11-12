package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;


@Entity
@org.hibernate.annotations.Filter(name = "empresa", condition = "{e}.empresa_id = :empresaId", deduceAliasInjectionPoints = false, aliases = @org.hibernate.annotations.SqlFragmentAlias(alias = "e", table = "configuracao_barbearia"))
@Table(name = "configuracao_barbearia", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "empresa_id"))
@Getter
@Setter
public class ConfiguracaoBarbearia extends bruninho.Barbeiro.platform.EmpresaScopedEntity {

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome = "Barbearia Bruninho";
    private String telefone;
    private String endereco;
    @PositiveOrZero
    @Max(30)
    private Integer diasMaximoAntecedentia;

    @PositiveOrZero
    @Max(30)
    private Integer diasToleranciaPagamentoPlano = 0;


}
