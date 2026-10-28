package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TenantRegistry implements AutoCloseable {
    public record Company(UUID id,String nome,String responsavel,String slug,String dbName,String dbUser,String encryptedPassword,String estado){}
    private final JdbcTemplate central;
    private final TenantSecrets secrets;
    private final String jdbcPrefix;
    private final String jdbcOptions;
    private final Map<UUID,HikariDataSource> pools=new ConcurrentHashMap<>();
    public TenantRegistry(DataSource central,TenantSecrets secrets,String url){
        this.central=new JdbcTemplate(central);this.secrets=secrets;
        // URL administrada pela instalacao; nomes de bancos sao gerados pelo servidor.
        var parsed=java.util.regex.Pattern.compile("jdbc:postgresql://([^/]+)/([a-zA-Z0-9_]+)(\\?.*)?").matcher(url);
        if(!parsed.matches())
            throw new IllegalStateException("Multiempresa requer DB_URL PostgreSQL no formato jdbc:postgresql://host:porta/banco");
        jdbcPrefix="jdbc:postgresql://"+parsed.group(1)+"/";
        jdbcOptions=parsed.group(3)==null?"":parsed.group(3);
    }
    public JdbcTemplate central(){return central;}
    public String url(String db){if(!db.matches("barber_t_[a-f0-9]{24}"))throw new IllegalArgumentException("Banco invalido");return jdbcPrefix+db+jdbcOptions;}
    public Optional<Company> find(String slug){return central.query("select * from platform_empresa where slug=?",(rs,n)->new Company(
            UUID.fromString(rs.getString("id")),rs.getString("nome"),rs.getString("responsavel"),rs.getString("slug"),
            rs.getString("db_name"),rs.getString("db_user"),rs.getString("db_password"),rs.getString("estado")),slug).stream().findFirst();}
    private Company row(java.sql.ResultSet rs)throws java.sql.SQLException{return new Company(UUID.fromString(rs.getString("id")),rs.getString("nome"),rs.getString("responsavel"),rs.getString("slug"),rs.getString("db_name"),rs.getString("db_user"),rs.getString("db_password"),rs.getString("estado"));}
    public List<Company> all(){return central.query("select * from platform_empresa order by criado_em desc",(rs,n)->row(rs));}
    public Company byId(UUID id){return central.query("select * from platform_empresa where id=?",(rs,n)->row(rs),id).stream().findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Empresa nao encontrada"));}
    public synchronized DataSource dataSource(Company c){
        return pools.computeIfAbsent(c.id(),id->{
            var config=new HikariConfig();config.setJdbcUrl(url(c.dbName()));config.setUsername(c.dbUser());config.setPassword(secrets.decrypt(c.encryptedPassword()));
            config.setMaximumPoolSize(5);config.setMinimumIdle(0);config.setConnectionTimeout(10000);config.setIdleTimeout(60000);
            config.setPoolName("empresa-"+id);var ds=new HikariDataSource(config);
            try{Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(true).load().migrate();return ds;}
            catch(RuntimeException e){ds.close();throw e;}
        });
    }
    public synchronized void evict(UUID id){var pool=pools.remove(id);if(pool!=null)pool.close();}
    @Override public synchronized void close(){pools.values().forEach(HikariDataSource::close);pools.clear();}
}
