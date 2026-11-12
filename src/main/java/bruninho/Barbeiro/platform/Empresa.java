package bruninho.Barbeiro.platform;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** Cadastro unico da empresa, compartilhado pelo painel e pelas barbearias. */
@Entity
@Table(name = "platform_empresa")
@Getter
@NoArgsConstructor
public class Empresa {
    @Id
    private UUID id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(nullable = false, length = 150)
    private String responsavel;
    @Column(nullable = false, unique = true, length = 40)
    private String slug;
    @Column(nullable = false, length = 20)
    private String estado;
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
    private Instant criadoEm;

    public boolean isAtiva() { return "ATIVA".equals(estado); }
}
