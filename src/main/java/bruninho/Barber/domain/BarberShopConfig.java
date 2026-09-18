package bruninho.Barber.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "barber_shop_config")
public class BarberShopConfig {
    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private String nome;
    private String telefone;
    private String endereco;
    private int minAntecedenciaMinutos;
    private int horizonteDias;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public int getMinAntecedenciaMinutos() { return minAntecedenciaMinutos; }
    public void setMinAntecedenciaMinutos(int minAntecedenciaMinutos) { this.minAntecedenciaMinutos = minAntecedenciaMinutos; }
    public int getHorizonteDias() { return horizonteDias; }
    public void setHorizonteDias(int horizonteDias) { this.horizonteDias = horizonteDias; }
}
