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
@Table(name = "agendamento")
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "codigo_confirmacao")
    private String codigoConfirmacao;
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;
    @ManyToOne(optional = false)
    @JoinColumn(name = "servico_id")
    private Servico servico;
    @ManyToOne
    @JoinColumn(name = "plano_mensal_id")
    private PlanoMensalCliente planoMensal;
    @Column(name = "nome_cliente")
    private String nomeCliente;
    @Column(name = "telefone_cliente")
    private String telefoneCliente;
    @Column(name = "nome_servico_snapshot")
    private String nomeServicoSnapshot;
    @Column(name = "preco_servico_snapshot", precision = 12, scale = 2)
    private BigDecimal precoServicoSnapshot;
    @Column(name = "duracao_servico_minutos_snapshot")
    private int duracaoServicoMinutosSnapshot;
    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;
    @Column(name = "fim_em")
    private LocalDateTime fimEm;
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;
    @Column(name = "pagamento_recebido")
    private boolean pagamentoRecebido;
    @ManyToOne
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPorUsuario;
    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    public Long getId() { return id; }
    public String getCodigoConfirmacao() { return codigoConfirmacao; }
    public void setCodigoConfirmacao(String codigoConfirmacao) { this.codigoConfirmacao = codigoConfirmacao; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public void setBarbeiro(Barbeiro barbeiro) { this.barbeiro = barbeiro; }
    public Servico getServico() { return servico; }
    public void setServico(Servico servico) { this.servico = servico; }
    public PlanoMensalCliente getPlanoMensal() { return planoMensal; }
    public void setPlanoMensal(PlanoMensalCliente planoMensal) { this.planoMensal = planoMensal; }
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    public String getTelefoneCliente() { return telefoneCliente; }
    public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }
    public String getNomeServicoSnapshot() { return nomeServicoSnapshot; }
    public void setNomeServicoSnapshot(String nomeServicoSnapshot) { this.nomeServicoSnapshot = nomeServicoSnapshot; }
    public BigDecimal getPrecoServicoSnapshot() { return precoServicoSnapshot; }
    public void setPrecoServicoSnapshot(BigDecimal precoServicoSnapshot) { this.precoServicoSnapshot = precoServicoSnapshot; }
    public int getDuracaoServicoMinutosSnapshot() { return duracaoServicoMinutosSnapshot; }
    public void setDuracaoServicoMinutosSnapshot(int duracaoServicoMinutosSnapshot) { this.duracaoServicoMinutosSnapshot = duracaoServicoMinutosSnapshot; }
    public LocalDateTime getInicioEm() { return inicioEm; }
    public void setInicioEm(LocalDateTime inicioEm) { this.inicioEm = inicioEm; }
    public LocalDateTime getFimEm() { return fimEm; }
    public void setFimEm(LocalDateTime fimEm) { this.fimEm = fimEm; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public boolean isPagamentoRecebido() { return pagamentoRecebido; }
    public void setPagamentoRecebido(boolean pagamentoRecebido) { this.pagamentoRecebido = pagamentoRecebido; }
    public Usuario getCriadoPorUsuario() { return criadoPorUsuario; }
    public void setCriadoPorUsuario(Usuario criadoPorUsuario) { this.criadoPorUsuario = criadoPorUsuario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void touch() { this.atualizadoEm = LocalDateTime.now(); }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
}
