package bruninho.Barbeiro.config;

import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepository;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

// Requer PostgreSQL vazio e exclusivo para este teste. Nunca use o banco da aplicacao.
@EnabledIfEnvironmentVariable(named="MIGRATION_TEST_DB_URL",matches="jdbc:postgresql:.*")
@SpringBootTest(properties={"spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate",
        "app.demo-data=false","app.initial-admin.login=","app.initial-admin.password="})
class FlywayMigrationIntegrationTest {
    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("MIGRATION_TEST_DB_URL"));
        r.add("spring.datasource.username",()->System.getenv("MIGRATION_TEST_DB_USERNAME"));
        r.add("spring.datasource.password",()->System.getenv("MIGRATION_TEST_DB_PASSWORD"));
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
    }
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;
    @Autowired ConfiguracaoBarbeariaRepository configuracoes;
    @Autowired HorarioFuncionamentoRepository horarios;

    @Test void criaBancoValidaEntidadesEPreservaDadosAoExecutarNovamente() throws Exception {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(horarios.count()).isZero();
        assertThat(configuracoes.count()).isZero();
        try(var connection=dataSource.getConnection();var statement=connection.createStatement();var result=statement.executeQuery("select count(*) from usuario")){
            assertThat(result.next()).isTrue();assertThat(result.getInt(1)).isZero();
        }
        var c=new bruninho.Barbeiro.domain.ConfiguracaoBarbearia();c.setNome("Empresa teste de migration");configuracoes.saveAndFlush(c);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(configuracoes.findById(1L).orElseThrow().getNome()).isEqualTo("Empresa teste de migration");
        try(var connection=dataSource.getConnection();var statement=connection.createStatement();
            var result=statement.executeQuery("select count(*) from flyway_schema_history where success=true")) {
            assertThat(result.next()).isTrue();assertThat(result.getInt(1)).isGreaterThan(0);
        }
    }
}
