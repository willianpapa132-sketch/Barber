package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "jornada_barbeiro")
public class JornadaBarbeiro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;
    @Column(name = "dia_semana")
    private int diaSemana;
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;
    @Column(name = "hora_fim")
    private LocalTime horaFim;
    @Column(name = "intervalo_inicio")
    private LocalTime intervaloInicio;
    @Column(name = "intervalo_fim")
    private LocalTime intervaloFim;
    @Column(name = "ativo")
    private boolean ativo = true;

    public Long getId() { return id; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public void setBarbeiro(Barbeiro barbeiro) { this.barbeiro = barbeiro; }
    public int getDiaSemana() { return diaSemana; }
    public void setDiaSemana(int diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public LocalTime getIntervaloInicio() { return intervaloInicio; }
    public void setIntervaloInicio(LocalTime intervaloInicio) { this.intervaloInicio = intervaloInicio; }
    public LocalTime getIntervaloFim() { return intervaloFim; }
    public void setIntervaloFim(LocalTime intervaloFim) { this.intervaloFim = intervaloFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
