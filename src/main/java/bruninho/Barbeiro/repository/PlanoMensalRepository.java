package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanosMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoMensalRepository extends JpaRepository <PlanosMensal, Long> {

    Boolean existsByServicosIncluidos_Id(Long servicoId);

    List<PlanosMensal> findAllByAtivoTrueOrderByValorMensalAsc();
}
