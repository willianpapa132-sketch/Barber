package bruninho.Barbeiro.domain;

import bruninho.Barbeiro.security.model.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @Column(nullable = false)
    @NotBlank(message = "deve ser informado o numero de telefone do barbeiro")
    private String telefone;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToOne(optional = false)
    private Usuario usuario;

    private String imgurl;




}
