package bruninho.Barber.web.form;

import jakarta.validation.constraints.*;

public class CustomerForm {
    @NotBlank @Size(max = 120) private String nome;
    @NotBlank @Size(max = 30) private String telefone;
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
