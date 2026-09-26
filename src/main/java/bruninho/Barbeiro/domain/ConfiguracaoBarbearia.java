package bruninho.Barbeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;

@Entity
@Table(name = "configuracao_barbearia")
public class ConfiguracaoBarbearia {

    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private String nome = "Barbearia Bruninho";
    private String telefone;
    private String endereco;


}
