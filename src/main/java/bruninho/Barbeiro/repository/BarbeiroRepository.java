package bruninho.Barbeiro.repository;

import bruninho.Barbeiro.domain.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select b from Barbeiro b
            where (:ativo is null or b.ativo = :ativo)
              and (lower(b.nome) like lower(concat('%', :busca, '%'))
                   or b.telefone like concat('%', :busca, '%'))
            """)
    org.springframework.data.domain.Page<Barbeiro> listarParaAdmin(
            @org.springframework.data.repository.query.Param("busca") String busca,
            @org.springframework.data.repository.query.Param("ativo") Boolean ativo,
            org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Barbeiro b where b.id = :id")
    java.util.Optional<Barbeiro> findByIdParaAgendar(@org.springframework.data.repository.query.Param("id") Long id);

    java.util.Optional<Barbeiro> findByUsuario_Login(String login);

    java.util.List<Barbeiro> findAllByAtivoTrueOrderByNomeAsc();
}
