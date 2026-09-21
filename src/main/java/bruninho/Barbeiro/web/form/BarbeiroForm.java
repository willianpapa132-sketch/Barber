package bruninho.Barbeiro.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

public class BarbeiroForm {
    @NotBlank @Size(max = 120) private String nome;
    @Size(max = 30) private String telefone;
    @NotBlank @Size(max = 80) private String login;
    @Size(min = 8, max = 120) private String password;
    private boolean ativo = true;
    private Set<Long> servicoIds = new HashSet<>();

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Set<Long> getServicoIds() { return servicoIds; }
    public void setServicoIds(Set<Long> servicoIds) { this.servicoIds = servicoIds; }
}
