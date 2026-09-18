package bruninho.Barber.repository;

import bruninho.Barber.domain.BarberBlock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarberBlockRepository extends JpaRepository<BarberBlock, Long> {
    List<BarberBlock> findByBarberIdAndBlockDate(Long barberId, LocalDate blockDate);
    List<BarberBlock> findByBarberIdOrderByBlockDateDescStartTimeDesc(Long barberId);
}
