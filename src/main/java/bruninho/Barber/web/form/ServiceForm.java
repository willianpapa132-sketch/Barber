package bruninho.Barber.web.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ServiceForm {
    @NotBlank @Size(max = 120) private String nome;
    @Size(max = 600) private String descricao;
    @NotNull @DecimalMin("0.00") private BigDecimal preco;
    @Min(1) private int duracaoMinutos;
    private boolean active = true;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(int duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
