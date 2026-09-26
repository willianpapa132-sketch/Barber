package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanoMensalCliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoMensalClienteRepository extends JpaRepository<PlanoMensalCliente,Long> {
}
