package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessao_caixa")
public class SessaoCaixa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime abertoEm = LocalDateTime.now();
    private LocalDateTime fechadoEm;
    @ManyToOne(optional = false)
    @JoinColumn(name = "aberto_por_usuario_id")
    private Usuario abertoPorUsuario;
    @ManyToOne
    @JoinColumn(name = "fechado_por_usuario_id")
    private Usuario fechadoPorUsuario;
    @Column(precision = 12, scale = 2)
    private BigDecimal dinheiroInicial;
    @Column(precision = 12, scale = 2)
    private BigDecimal dinheiroEsperado;
    @Column(precision = 12, scale = 2)
    private BigDecimal dinheiroContado;
    @Column(precision = 12, scale = 2)
    private BigDecimal diferencaDinheiro;
    @Enumerated(EnumType.STRING)
    private StatusSessaoCaixa status = StatusSessaoCaixa.ABERTO;

    public Long getId() { return id; }
    public LocalDateTime getAbertoEm() { return abertoEm; }
    public LocalDateTime getFechadoAt() { return fechadoEm; }
    public void setFechadoAt(LocalDateTime fechadoEm) { this.fechadoEm = fechadoEm; }
    public Usuario getAbertoPorUsuario() { return abertoPorUsuario; }
    public void setAbertoPorUsuario(Usuario abertoPorUsuario) { this.abertoPorUsuario = abertoPorUsuario; }
    public Usuario getFechadoByUser() { return fechadoPorUsuario; }
    public void setFechadoByUser(Usuario fechadoPorUsuario) { this.fechadoPorUsuario = fechadoPorUsuario; }
    public BigDecimal getDinheiroInicial() { return dinheiroInicial; }
    public void setDinheiroInicial(BigDecimal dinheiroInicial) { this.dinheiroInicial = dinheiroInicial; }
    public BigDecimal getDinheiroEsperado() { return dinheiroEsperado; }
    public void setDinheiroEsperado(BigDecimal dinheiroEsperado) { this.dinheiroEsperado = dinheiroEsperado; }
    public BigDecimal getDinheiroContado() { return dinheiroContado; }
    public void setDinheiroContado(BigDecimal dinheiroContado) { this.dinheiroContado = dinheiroContado; }
    public BigDecimal getDiferencaDinheiro() { return diferencaDinheiro; }
    public void setDiferencaDinheiro(BigDecimal diferencaDinheiro) { this.diferencaDinheiro = diferencaDinheiro; }
    public StatusSessaoCaixa getStatus() { return status; }
    public void setStatus(StatusSessaoCaixa status) { this.status = status; }
}
