package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.BloqueioBarbeiro;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueioBarbeiroRepositorio extends JpaRepository<BloqueioBarbeiro, Long> {
    List<BloqueioBarbeiro> findByBarbeiroIdAndDataBloqueio(Long barbeiroId, LocalDate dataBloqueio);
    List<BloqueioBarbeiro> findByBarbeiroIdOrderByDataBloqueioDescHoraInicioDesc(Long barbeiroId);
}
