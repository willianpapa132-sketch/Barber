package bruninho.Barbeiro.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@org.hibernate.annotations.Filter(name = "empresa", condition = "{e}.empresa_id = :empresaId", deduceAliasInjectionPoints = false, aliases = @org.hibernate.annotations.SqlFragmentAlias(alias = "e", table = "horario_funcionamento"))
@Table(name = "horario_funcionamento", uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "dia_semana"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioFuncionamento extends bruninho.Barbeiro.platform.EmpresaScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "hora_abertura")
    private LocalTime horaAbertura;

    @Column(name = "hora_fechamento")
    private LocalTime horaFechamento;

    @Column(name = "fechado")
    private Boolean fechado;

    @Column(name = "permite_agendamento_plano")
    private Boolean permiteAgendamentoPlano = true;

    @Positive
    private Integer intervaloMinimoEntreAgendamentos;



}
