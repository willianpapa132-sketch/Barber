package bruninho.Barber.web.form;

import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;

public class BarberForm {
    @NotBlank @Size(max = 120) private String nome;
    @Size(max = 30) private String telefone;
    @NotBlank @Size(max = 80) private String username;
    @Size(min = 8, max = 120) private String password;
    private boolean active = true;
    private Set<Long> serviceIds = new HashSet<>();

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(Set<Long> serviceIds) { this.serviceIds = serviceIds; }
}
