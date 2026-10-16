package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuario_Login(String login);

    @org.springframework.data.jpa.repository.Query("""
            select c from Cliente c where c.usuario.ativo = true
            and (lower(c.nome) like lower(concat('%', :busca, '%')) or c.telefone like concat('%', :busca, '%'))
            order by c.nome asc, c.id asc
            """)
    java.util.List<Cliente> buscarParaAgenda(@org.springframework.data.repository.query.Param("busca") String busca,
                                            org.springframework.data.domain.Pageable pageable);
}
