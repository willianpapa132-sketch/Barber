package bruninho.Barbeiro.repository;
import bruninho.Barbeiro.domain.Comanda;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface ComandaRepository extends JpaRepository<Comanda,Long> {
    Optional<Comanda> findByAgendamento_Id(Long id);
    boolean existsByAgendamento_Id(Long id);
    @Query("select c from Comanda c where c.fechadoEm >= :inicio and c.fechadoEm < :fim order by c.fechadoEm desc,c.id desc")
    List<Comanda> periodo(@Param("inicio") LocalDateTime inicio,@Param("fim") LocalDateTime fim);
    @Query("select coalesce(sum(c.minutos),0) from Comanda c where c.barbeiro.id=:barbeiroId and cast(c.fechadoEm as date) between :inicio and :fim")
    long somarMinutosConcluidos(@Param("barbeiroId") Long barbeiroId,@Param("inicio") java.time.LocalDate inicio,@Param("fim") java.time.LocalDate fim);
    @Query("select coalesce(sum(c.total),0) from Comanda c where cast(c.fechadoEm as date)=:data")
    java.math.BigDecimal somarValoresConcluidos(@Param("data") java.time.LocalDate data);
    @Query("select count(c) from Comanda c where c.barbeiro.id=:barbeiroId and cast(c.fechadoEm as date)=:data")
    long contarFechadas(@Param("barbeiroId") Long barbeiroId,@Param("data") java.time.LocalDate data);
    boolean existsByItens_ProdutoId(Long id);
    boolean existsByItens_ServicoId(Long id);
}
