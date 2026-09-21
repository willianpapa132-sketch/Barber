package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.SessaoCaixa;
import bruninho.Barbeiro.domain.StatusSessaoCaixa;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SessaoCaixaRepositorio extends JpaRepository<SessaoCaixa, Long> {
    Optional<SessaoCaixa> findByStatus(StatusSessaoCaixa status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SessaoCaixa c where c.status = bruninho.Barbeiro.domain.StatusSessaoCaixa.ABERTO")
    Optional<SessaoCaixa> lockOpenSession();
}
