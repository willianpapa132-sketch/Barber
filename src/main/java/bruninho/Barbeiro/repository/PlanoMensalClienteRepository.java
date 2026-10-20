package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.security.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoMensalClienteRepository extends JpaRepository<PlanoMensalCliente,Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cliente", "planoMensal"})
    List<PlanoMensalCliente> findAllByOrderByCriadoEmDesc();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PlanoMensalCliente p where p.id = :id")
    Optional<PlanoMensalCliente> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select p.id from PlanoMensalCliente p where p.ativo = true order by p.id")
    List<Long> idsAtivos();

    Boolean existsByPlanoMensal_id(Long planoMensalId);
    List <PlanoMensalCliente> findAllByPlanoMensal_Id(Long id);
    Boolean existsByCliente_id(Long cliente_id);
    Optional<PlanoMensalCliente> findByCliente_IdAndAtivoTrue(Long clienteId);
    Boolean existsByUsuarioAndAtivoTrue(Usuario usuario);
    PlanoMensalCliente findByUsuarioAndAtivoTrue(Usuario usuario);

}
