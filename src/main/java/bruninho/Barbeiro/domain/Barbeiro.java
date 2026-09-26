package bruninho.Barbeiro.domain;

import bruninho.Barbeiro.security.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Entity
@Table(name = "barbeiro")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Barbeiro {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String telefone;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToOne(optional = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "barbeiro")
    private List <PlanoMensalCliente> planoMensalCliente;

}
