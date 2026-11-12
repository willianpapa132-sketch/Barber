package bruninho.Barbeiro.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.*;

/** Resolve empresas no unico DataSource da aplicacao. Nao mantem pools ou credenciais. */
public final class TenantRegistry {
    public record Company(UUID id, String nome, String responsavel, String slug, String estado) {}
    private final JdbcTemplate central;
    public TenantRegistry(DataSource dataSource) { central = new JdbcTemplate(dataSource); }
    public JdbcTemplate central() { return central; }
    private Company row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Company(UUID.fromString(rs.getString("id")), rs.getString("nome"),
                rs.getString("responsavel"), rs.getString("slug"), rs.getString("estado"));
    }
    public Optional<Company> find(String slug) {
        return central.query("select id,nome,responsavel,slug,estado from platform_empresa where slug=?",
                (rs,n) -> row(rs), slug).stream().findFirst();
    }
    public List<Company> all() {
        return central.query("select id,nome,responsavel,slug,estado from platform_empresa order by criado_em desc", (rs,n) -> row(rs));
    }
    public Company byId(UUID id) {
        return central.query("select id,nome,responsavel,slug,estado from platform_empresa where id=?", (rs,n) -> row(rs), id)
                .stream().findFirst().orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
    }
}
