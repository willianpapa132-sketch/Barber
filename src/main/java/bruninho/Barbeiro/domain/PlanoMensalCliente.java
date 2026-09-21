package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plano_mensal_cliente")
public class PlanoMensalCliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;
    @Column(name = "valor_mensal_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensalSnapshot;
    @Column(name = "cortes_por_mes_snapshot", nullable = false)
    private int cortesPorMesSnapshot;
    @Column(name = "cortes_por_semana_snapshot", nullable = false)
    private int cortesPorSemanaSnapshot;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;
    @Column(name = "data_fim")
    private LocalDate dataFim;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public void setBarbeiro(Barbeiro barbeiro) { this.barbeiro = barbeiro; }
    public BigDecimal getValorMensalSnapshot() { return valorMensalSnapshot; }
    public void setValorMensalSnapshot(BigDecimal valorMensalSnapshot) { this.valorMensalSnapshot = valorMensalSnapshot; }
    public int getCortesPorMesSnapshot() { return cortesPorMesSnapshot; }
    public void setCortesPorMesSnapshot(int cortesPorMesSnapshot) { this.cortesPorMesSnapshot = cortesPorMesSnapshot; }
    public int getCortesPorSemanaSnapshot() { return cortesPorSemanaSnapshot; }
    public void setCortesPorSemanaSnapshot(int cortesPorSemanaSnapshot) { this.cortesPorSemanaSnapshot = cortesPorSemanaSnapshot; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void touch() { this.atualizadoEm = LocalDateTime.now(); }
}
