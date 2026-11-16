package bruninho.Barbeiro.repository;
import bruninho.Barbeiro.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Produto p where p.id=:id")
    java.util.Optional<Produto> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("select p from Produto p where lower(p.nome) like lower(concat('%', :busca, '%'))")
    Page<Produto> listarParaAdmin(@Param("busca") String busca, Pageable pagina);
}
