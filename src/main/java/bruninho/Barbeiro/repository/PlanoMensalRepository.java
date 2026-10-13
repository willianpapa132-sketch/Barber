package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanosMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoMensalRepository extends JpaRepository <PlanosMensal, Long> {

    Boolean existsByServicosIncluidos_Id(Long servicoId);

    List<PlanosMensal> findAllByAtivoTrueOrderByValorMensalAsc();

    Optional<PlanosMensal> findByNomePlanoIgnoreCase(String nomePlano);
}
