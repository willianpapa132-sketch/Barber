package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.BloqueioBarbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BloqueioBarbeiroRepository extends JpaRepository<BloqueioBarbeiro,Long> {

    boolean existsByDataBloqueioAndBarbeiro_Id(LocalDate diaBloqueio, Long barbeiroId);
}
