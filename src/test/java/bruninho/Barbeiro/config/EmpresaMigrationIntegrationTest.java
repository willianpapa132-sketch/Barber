package bruninho.Barbeiro.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Cada teste usa um schema aleatorio em um PostgreSQL exclusivamente de testes. */
@EnabledIfEnvironmentVariable(named="MIGRATION_TEST_DB_URL", matches="jdbc:postgresql:.*")
class EmpresaMigrationIntegrationTest {
    private String url() { return System.getenv("MIGRATION_TEST_DB_URL"); }
    private String user() { return System.getenv("MIGRATION_TEST_DB_USERNAME"); }
    private String password() { return System.getenv("MIGRATION_TEST_DB_PASSWORD"); }
    private String schema() { return "tenant_test_"+UUID.randomUUID().toString().replace("-", ""); }
    private Flyway migration(String schema,String empresa,String target) {
        var config=Flyway.configure().dataSource(url(),user(),password()).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").placeholders(Map.of("legacyEmpresaId",empresa));
        if(target!=null)config.target(target);
        return config.load();
    }
    private Connection connection(String schema) throws SQLException {
        var c=DriverManager.getConnection(url(),user(),password());c.setSchema(schema);return c;
    }
    private void empresa(Statement s, UUID id, String slug) throws SQLException {
        s.executeUpdate("insert into platform_empresa(id,nome,responsavel,slug,estado) values ('"+id+"','Teste','Teste','"+slug+"','ATIVA')");
    }
    private long id(Statement s,String sql) throws SQLException {
        try(var rs=s.executeQuery(sql+" returning id")){assertThat(rs.next()).isTrue();return rs.getLong(1);}
    }
    @Test void criaBancoCompartilhadoEImpedeVinculosCruzadosNoPostgreSQL() throws Exception {
        var schema=schema();var flyway=migration(schema,"",null);flyway.migrate();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try(var c=connection(schema);var s=c.createStatement()) {
            var a=UUID.randomUUID();var b=UUID.randomUUID();empresa(s,a,"alfa");empresa(s,b,"beta");
            long usuario=id(s,"insert into usuario(empresa_id,login,senha,role,ativo) values ('"+a+"','mesmo','hash','ADMIN',true)");
            s.executeUpdate("insert into usuario(empresa_id,login,senha,role,ativo) values ('"+b+"','mesmo','hash','ADMIN',true)");
            assertThatThrownBy(()->s.executeUpdate("insert into usuario(empresa_id,login,senha) values ('"+a+"','MESMO','hash')"))
                    .isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23505");
            assertThatThrownBy(()->s.executeUpdate("insert into cliente(empresa_id,usuario_id,nome,telefone) values ('"+b+"',"+usuario+",'Invasao','119999999')"))
                    .isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23514");
            long servico=id(s,"insert into servico(empresa_id,nome,ativo,duracao_minutos,preco) values ('"+b+"','Corte',true,30,10)");
            long plano=id(s,"insert into plano_mensal(empresa_id,nome_plano,descricao,ativo,valor_mensal) values ('"+a+"','Plano','Teste',true,100)");
            assertThatThrownBy(()->s.executeUpdate("insert into plano_servicos(plano_mensal_id,servico_id) values ("+plano+","+servico+")"))
                    .isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23514");
            long desconto=id(s,"insert into servicos_planos_desconto(plano_mensal_id,porcentagem_desconto) values ("+plano+",10)");
            assertThatThrownBy(()->s.executeUpdate("insert into servicos_planos_desconto_servicos(desconto_id,servico_id) values ("+desconto+","+servico+")"))
                    .isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23514");
            assertThatThrownBy(()->s.executeUpdate("update servico set empresa_id='"+a+"' where id="+servico))
                    .isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23514");
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
    @Test void legadoExigeEmpresaExplicitaEPreservaDadosEUuid() throws Exception {
        var schema=schema();migration(schema,"","1").migrate();
        try(var c=connection(schema);var s=c.createStatement()) {
            s.executeUpdate("insert into servico(nome,ativo,duracao_minutos,preco) values ('Legado',true,30,10)");
        }
        assertThatThrownBy(()->migration(schema,"",null).migrate()).hasMessageContaining("Dados legados sem empresa");
        var empresa=UUID.randomUUID();
        try(var c=connection(schema);var s=c.createStatement()) {
            // O cadastro central antigo e importado preservando o UUID da empresa.
            try(var resource=getClass().getResourceAsStream("/db/platform/V1__cadastro_plataforma.sql")) {
                s.execute(new String(Objects.requireNonNull(resource).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
            s.executeUpdate("insert into platform_empresa(id,nome,responsavel,slug,estado,db_name,db_user,db_password) values ('"+empresa+"','Legado','Teste','legado','ATIVA','antigo','antigo','backup')");
        }
        var flyway=migration(schema,empresa.toString(),null);flyway.migrate();
        try(var c=connection(schema);var s=c.createStatement();var rs=s.executeQuery("select nome,empresa_id from servico")) {
            assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).isEqualTo("Legado");assertThat(rs.getObject(2,UUID.class)).isEqualTo(empresa);
        }
        try(var c=connection(schema);var s=c.createStatement();var rs=s.executeQuery("select count(*) from information_schema.columns where table_schema=current_schema() and table_name='platform_empresa' and column_name in ('db_name','db_user','db_password')")) {
            rs.next();assertThat(rs.getInt(1)).isZero();
        }
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }
}
