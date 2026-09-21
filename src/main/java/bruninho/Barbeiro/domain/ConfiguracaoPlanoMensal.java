package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracao_plano_mensal")
public class ConfiguracaoPlanoMensal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;
    @Column(name = "valor_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensal = BigDecimal.ZERO;
    @Column(name = "cortes_por_mes", nullable = false)
    private int cortesPorMes = 4;
    @Column(name = "cortes_por_semana", nullable = false)
    private int cortesPorSemana = 1;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public void setBarbeiro(Barbeiro barbeiro) { this.barbeiro = barbeiro; }
    public BigDecimal getValorMensal() { return valorMensal; }
    public void setValorMensal(BigDecimal valorMensal) { this.valorMensal = valorMensal; }
    public int getCortesPorMes() { return cortesPorMes; }
    public void setCortesPorMes(int cortesPorMes) { this.cortesPorMes = cortesPorMes; }
    public int getCortesPorSemana() { return cortesPorSemana; }
    public void setCortesPorSemana(int cortesPorSemana) { this.cortesPorSemana = cortesPorSemana; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void touch() { this.atualizadoEm = LocalDateTime.now(); }
}
