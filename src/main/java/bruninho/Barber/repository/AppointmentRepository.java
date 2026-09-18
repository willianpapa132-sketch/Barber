package bruninho.Barber.repository;

import bruninho.Barber.domain.Appointment;
import bruninho.Barber.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("""
        select a from Appointment a
        where a.barber.id = :barberId
          and a.status in :blockingStatuses
          and a.startAt < :endAt
          and a.endAt > :startAt
        order by a.startAt
    """)
    List<Appointment> findConflicts(@Param("barberId") Long barberId,
                                    @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt,
                                    @Param("blockingStatuses") Collection<AppointmentStatus> blockingStatuses);

    @Query("""
        select a from Appointment a
        where a.barber.id = :barberId and a.startAt >= :from and a.startAt < :to
        order by a.startAt
    """)
    List<Appointment> findAgenda(@Param("barberId") Long barberId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select a from Appointment a
        where (:barberId is null or a.barber.id = :barberId)
          and a.startAt >= :from and a.startAt < :to
          and (:status is null or a.status = :status)
        order by a.startAt
    """)
    List<Appointment> search(@Param("barberId") Long barberId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("status") AppointmentStatus status);

    List<Appointment> findByCustomerIdOrderByStartAtDesc(Long customerId);
    long countByStatusAndStartAtBetween(AppointmentStatus status, LocalDateTime from, LocalDateTime to);
    List<Appointment> findTop20ByStartAtBetweenOrderByStartAt(LocalDateTime from, LocalDateTime to);
}
