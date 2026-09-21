package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanoMensalCliente;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanoMensalClienteRepositorio extends JpaRepository<PlanoMensalCliente, Long> {
    @Query("""
        select p from PlanoMensalCliente p
        join fetch p.cliente c
        join fetch p.barbeiro b
        where c.telefoneNormalizado = :telefone
          and b.id = :barbeiroId
          and p.ativo = true
          and p.dataInicio <= :data
          and (p.dataFim is null or p.dataFim >= :data)
        order by p.dataInicio desc
    """)
    List<PlanoMensalCliente> buscarAtivoPorTelefoneEBarbeiro(@Param("telefone") String telefoneNormalizado,
                                                             @Param("barbeiroId") Long barbeiroId,
                                                             @Param("data") LocalDate data);

    @Query("select p from PlanoMensalCliente p join fetch p.cliente c join fetch p.barbeiro b where p.ativo = true order by b.nome, c.nome")
    List<PlanoMensalCliente> buscarPlanosAtivos();

    Optional<PlanoMensalCliente> findByIdAndAtivoTrue(Long id);
}
