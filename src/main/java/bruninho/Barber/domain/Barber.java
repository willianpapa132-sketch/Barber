package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "barber")
public class Barber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    private String telefone;
    @Column(nullable = false)
    private boolean active = true;
    @OneToOne(optional = false)
    private AppUser user;
    @Version
    private Long version;
    @ManyToMany
    @JoinTable(name = "barber_service", joinColumns = @JoinColumn(name = "barber_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceCatalog> services = new HashSet<>();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Set<ServiceCatalog> getServices() { return services; }
}
