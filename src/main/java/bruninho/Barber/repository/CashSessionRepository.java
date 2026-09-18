package bruninho.Barber.repository;

import bruninho.Barber.domain.CashSession;
import bruninho.Barber.domain.CashSessionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {
    Optional<CashSession> findByStatus(CashSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashSession c where c.status = bruninho.Barber.domain.CashSessionStatus.ABERTO")
    Optional<CashSession> lockOpenSession();
}
