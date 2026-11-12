package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@jakarta.persistence.EntityListeners(bruninho.Barbeiro.platform.EmpresaEntityListener.class)
@org.hibernate.annotations.Filter(name = "empresa", condition = "{e}.barbeiro_id in (select p.id from barbeiro p where p.empresa_id = :empresaId)", deduceAliasInjectionPoints = false, aliases = @org.hibernate.annotations.SqlFragmentAlias(alias = "e", table = "jornada_barbeiro"))
@Table(name = "jornada_barbeiro", uniqueConstraints = @UniqueConstraint(name = "uk_jornada_barbeiro_dia", columnNames = {"barbeiro_id", "dia_semana"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JornadaBarbeiro {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "intervalo_inicio")
    private LocalTime intervaloInicio;

    @Column(name = "intervalo_fim")
    private LocalTime intervaloFim;

    @Column(name = "ativo")
    private Boolean ativo ;

    private Boolean folga;


}
