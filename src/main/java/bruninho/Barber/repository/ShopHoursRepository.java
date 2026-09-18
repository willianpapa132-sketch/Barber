package bruninho.Barber.repository;

import bruninho.Barber.domain.ShopHours;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopHoursRepository extends JpaRepository<ShopHours, Long> {
    Optional<ShopHours> findByDayOfWeek(int dayOfWeek);
}
