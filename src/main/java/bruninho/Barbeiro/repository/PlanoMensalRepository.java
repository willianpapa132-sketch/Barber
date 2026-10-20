package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.PlanosMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoMensalRepository extends JpaRepository <PlanosMensal, Long> {

    @org.springframework.data.jpa.repository.Query("select p from PlanosMensal p where lower(p.nomePlano) like lower(concat('%', :busca, '%')) and (:ativo is null or p.ativo = :ativo)")
    org.springframework.data.domain.Page<PlanosMensal> listarParaAdmin(
            @org.springframework.data.repository.query.Param("busca") String busca,
            @org.springframework.data.repository.query.Param("ativo") Boolean ativo,
            org.springframework.data.domain.Pageable pageable);

    Boolean existsByServicosIncluidos_Id(Long servicoId);

    List<PlanosMensal> findAllByAtivoTrueOrderByValorMensalAsc();

    Optional<PlanosMensal> findByNomePlanoIgnoreCase(String nomePlano);
}
