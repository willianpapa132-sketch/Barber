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
@Table(name = "movimento_caixa")
public class MovimentoCaixa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "sessao_caixa_id")
    private SessaoCaixa sessaoCaixa;
    @ManyToOne
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;
    @ManyToOne
    @JoinColumn(name = "movimento_original_id")
    private MovimentoCaixa movimentoOriginal;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoMovimentoCaixa tipo;
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;
    @Column(name = "valor", precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "categoria")
    private String categoria;
    @ManyToOne(optional = false)
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPorUsuario;
    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "motivo_estorno")
    private String motivoEstorno;
    @Column(name = "estornado")
    private boolean estornado;

    public Long getId() { return id; }
    public SessaoCaixa getSessaoCaixa() { return sessaoCaixa; }
    public void setSessaoCaixa(SessaoCaixa sessaoCaixa) { this.sessaoCaixa = sessaoCaixa; }
    public Agendamento getAgendamento() { return agendamento; }
    public void setAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }
    public MovimentoCaixa getMovimentoOriginal() { return movimentoOriginal; }
    public void setMovimentoOriginal(MovimentoCaixa movimentoOriginal) { this.movimentoOriginal = movimentoOriginal; }
    public TipoMovimentoCaixa getTipo() { return tipo; }
    public void setTipo(TipoMovimentoCaixa tipo) { this.tipo = tipo; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Usuario getCriadoPorUsuario() { return criadoPorUsuario; }
    public void setCriadoPorUsuario(Usuario criadoPorUsuario) { this.criadoPorUsuario = criadoPorUsuario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public String getMotivoEstorno() { return motivoEstorno; }
    public void setMotivoEstorno(String motivoEstorno) { this.motivoEstorno = motivoEstorno; }
    public boolean isEstornado() { return estornado; }
    public void setEstornado(boolean estornado) { this.estornado = estornado; }
}
