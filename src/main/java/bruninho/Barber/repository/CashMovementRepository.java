package bruninho.Barber.repository;

import bruninho.Barber.domain.CashMovement;
import bruninho.Barber.domain.CashMovementType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    Optional<CashMovement> findByAppointmentIdAndTypeAndReversedFalse(Long appointmentId, CashMovementType type);
    List<CashMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<CashMovement> findByCashSessionIdOrderByCreatedAt(Long cashSessionId);
}
