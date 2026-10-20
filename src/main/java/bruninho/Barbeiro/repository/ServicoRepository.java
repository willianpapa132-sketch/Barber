package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @org.springframework.data.jpa.repository.Query("select s from Servico s where lower(s.nome) like lower(concat('%', :busca, '%'))")
    org.springframework.data.domain.Page<Servico> listarParaAdmin(@org.springframework.data.repository.query.Param("busca") String busca, org.springframework.data.domain.Pageable pagina);

    Optional<Servico> findByNomeIgnoreCase(String nome);

    java.util.List<Servico> findAllByAtivoTrueOrderByNomeAsc();
}
