package bruninho.Barbeiro.web.form;

import bruninho.Barbeiro.domain.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CaixaForms {
    public static class OpenCashForm {
        @NotNull @DecimalMin("0.00") private BigDecimal dinheiroInicial;
        public BigDecimal getDinheiroInicial() { return dinheiroInicial; }
        public void setDinheiroInicial(BigDecimal dinheiroInicial) { this.dinheiroInicial = dinheiroInicial; }
    }
    public static class ReceiptForm {
        @NotNull private Long agendamentoId;
        @NotNull private FormaPagamento formaPagamento;
        public Long getAgendamentoId() { return agendamentoId; }
        public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
        public FormaPagamento getFormaPagamento() { return formaPagamento; }
        public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    }
    public static class ManualMovementForm {
        @NotNull @DecimalMin("0.01") private BigDecimal valor;
        @NotBlank @Size(max = 255) private String descricao;
        @Size(max = 80) private String categoria;
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
    }
    public static class CloseCashForm {
        @NotNull @DecimalMin("0.00") private BigDecimal dinheiroContado;
        public BigDecimal getDinheiroContado() { return dinheiroContado; }
        public void setDinheiroContado(BigDecimal dinheiroContado) { this.dinheiroContado = dinheiroContado; }
    }
    public static class ReversalForm {
        @NotBlank @Size(max = 255) private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
