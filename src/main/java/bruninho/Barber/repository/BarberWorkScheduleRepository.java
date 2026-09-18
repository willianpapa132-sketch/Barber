package bruninho.Barber.repository;

import bruninho.Barber.domain.BarberWorkSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarberWorkScheduleRepository extends JpaRepository<BarberWorkSchedule, Long> {
    @Query("select s from BarberWorkSchedule s where s.barber.id = :barberId and s.dayOfWeek = :dayOfWeek and s.active = true")
    Optional<BarberWorkSchedule> findByBarberIdAndDayOfWeekAndActiveTrue(@Param("barberId") Long barberId, @Param("dayOfWeek") int dayOfWeek);
    List<BarberWorkSchedule> findByBarberIdOrderByDayOfWeek(Long barberId);
    void deleteByBarberId(Long barberId);
}
