package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanoMensalCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoMensalClienteRepository extends JpaRepository<PlanoMensalCliente,Long> {

    Boolean existsByPlanoMensal_id(Long planoMensalId);
    List <PlanoMensalCliente> findAllByPlanoMensal_Id(Long id);


    Optional<PlanoMensalCliente> findByCliente_id(Long cliente_id);
    Boolean existsByCliente_id(Long cliente_id);
}
