package bruninho.Barber.repository;

import bruninho.Barber.domain.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findTop30ByNomeContainingIgnoreCaseOrTelefoneNormalizadoContainingOrderByNome(String nome, String telefone);
}
