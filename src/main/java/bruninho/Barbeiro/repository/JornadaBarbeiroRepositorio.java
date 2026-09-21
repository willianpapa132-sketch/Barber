package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.JornadaBarbeiro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JornadaBarbeiroRepositorio extends JpaRepository<JornadaBarbeiro, Long> {
    @Query("select s from JornadaBarbeiro s where s.barbeiro.id = :barbeiroId and s.diaSemana = :diaSemana and s.ativo = true")
    Optional<JornadaBarbeiro> findByBarbeiroIdAndDiaSemanaAndAtivoTrue(@Param("barbeiroId") Long barbeiroId, @Param("diaSemana") int diaSemana);
    List<JornadaBarbeiro> findByBarbeiroIdOrderByDiaSemana(Long barbeiroId);
    void deleteByBarbeiroId(Long barbeiroId);
}
