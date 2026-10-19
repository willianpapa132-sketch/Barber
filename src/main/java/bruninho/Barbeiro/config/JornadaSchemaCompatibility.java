package bruninho.Barbeiro.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/** Atualiza bancos legados quando a aplicacao usa o modo automatico de schema. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto", havingValue = "update")
public class JornadaSchemaCompatibility implements ApplicationRunner {
    private final DataSource dataSource;
    public JornadaSchemaCompatibility(DataSource dataSource) { this.dataSource = dataSource; }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) return;
            var sql = new ClassPathResource("db/compat/jornada-barbeiro.sql").getContentAsString(StandardCharsets.UTF_8);
            try (var statement = connection.createStatement()) { statement.execute(sql); }
        }
    }
}
