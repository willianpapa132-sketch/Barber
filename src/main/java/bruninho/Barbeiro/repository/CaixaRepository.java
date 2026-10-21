package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CaixaRepository extends JpaRepository<Caixa, Long> {
    Optional<Caixa> findFirstByStatusOrderByIdDesc(StatusCaixa status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.status = bruninho.Barbeiro.domain.StatusCaixa.ABERTO")
    Optional<Caixa> findAberto();
}
