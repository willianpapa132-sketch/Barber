package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false)
    private String telefone;
    @Column(nullable = false)
    private String telefoneNormalizado;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getTelefoneNormalizado() { return telefoneNormalizado; }
    public void setTelefoneNormalizado(String telefoneNormalizado) { this.telefoneNormalizado = telefoneNormalizado; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = LocalDateTime.now(); }
}
