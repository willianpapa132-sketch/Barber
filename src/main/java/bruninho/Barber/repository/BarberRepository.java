package bruninho.Barber.repository;

import bruninho.Barber.domain.Barber;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarberRepository extends JpaRepository<Barber, Long> {
    List<Barber> findByActiveTrueOrderByNome();
    List<Barber> findAllByOrderByNome();
    Optional<Barber> findByUserUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barber b where b.id = :id")
    Optional<Barber> lockById(@Param("id") Long id);

    @Query("select distinct b from Barber b join b.services s where b.active = true and s.id = :serviceId and s.active = true order by b.nome")
    List<Barber> findActiveByService(@Param("serviceId") Long serviceId);
}
