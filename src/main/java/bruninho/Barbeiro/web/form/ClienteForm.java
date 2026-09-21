package bruninho.Barbeiro.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteForm {
    @NotBlank @Size(max = 120) private String nome;
    @NotBlank @Size(max = 30) private String telefone;
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
