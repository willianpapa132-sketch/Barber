package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "horario_funcionamento")
public class HorarioFuncionamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dia_semana")
    private int diaSemana;
    @Column(name = "hora_abertura")
    private LocalTime horaAbertura;
    @Column(name = "hora_fechamento")
    private LocalTime horaFechamento;
    @Column(name = "fechado")
    private boolean fechado;

    public Long getId() { return id; }
    public int getDiaSemana() { return diaSemana; }
    public void setDiaSemana(int diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraAbertura() { return horaAbertura; }
    public void setHoraAbertura(LocalTime horaAbertura) { this.horaAbertura = horaAbertura; }
    public LocalTime getHoraFechamento() { return horaFechamento; }
    public void setHoraFechamento(LocalTime horaFechamento) { this.horaFechamento = horaFechamento; }
    public boolean isFechado() { return fechado; }
    public void setFechado(boolean fechado) { this.fechado = fechado; }
}
