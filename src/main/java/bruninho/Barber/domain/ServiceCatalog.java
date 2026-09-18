package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_catalog")
public class ServiceCatalog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(length = 600)
    private String descricao;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false)
    private int duracaoMinutos;
    @Column(nullable = false)
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = LocalDateTime.now(); }
}
