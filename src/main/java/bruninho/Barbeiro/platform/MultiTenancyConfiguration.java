package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true")
public class MultiTenancyConfiguration {
    @Bean(name="platformDataSource",defaultCandidate=false,destroyMethod="close")
    HikariDataSource central(@Value("${spring.datasource.url}")String url,@Value("${spring.datasource.username}")String user,@Value("${spring.datasource.password}")String password){
        var c=new HikariConfig();c.setJdbcUrl(url);c.setUsername(user);c.setPassword(password);c.setMaximumPoolSize(5);c.setPoolName("plataforma");return new HikariDataSource(c);
    }
    @Bean TenantSecrets tenantSecrets(AppProperties p){return new TenantSecrets(p);}
    @Bean(destroyMethod="close") TenantRegistry tenantRegistry(@Qualifier("platformDataSource")DataSource ds,TenantSecrets secrets,@Value("${spring.datasource.url}")String url){return new TenantRegistry(ds,secrets,url);}
    @Bean(name="dataSource") @Primary TenantRoutingDataSource routing(TenantRegistry registry,@Qualifier("platformDataSource")DataSource ds){return new TenantRoutingDataSource(registry,ds);}
    @Bean FlywayMigrationStrategy platformMigrations(@Qualifier("platformDataSource")DataSource ds){return flyway->{
        flyway.migrate();
        Flyway.configure().dataSource(ds).locations("classpath:db/platform").table("platform_schema_history").baselineOnMigrate(true).baselineVersion("0").cleanDisabled(true).load().migrate();
        // O esquema central tambem serve de referencia para validacao inicial das entidades.
    };}
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<TenantRequestFilter> noServletRegistration(TenantRequestFilter filter){
        var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }
    @Bean PlatformBootstrap platformBootstrap(TenantRegistry registry,AppProperties p,PasswordEncoder encoder,TenantRoutingDataSource ds){return new PlatformBootstrap(registry,p,encoder,ds);}
    public static class PlatformBootstrap {
        private final TenantRegistry registry;private final AppProperties props;private final PasswordEncoder encoder;private final TenantRoutingDataSource routing;
        PlatformBootstrap(TenantRegistry r,AppProperties p,PasswordEncoder e,TenantRoutingDataSource ds){registry=r;props=p;encoder=e;routing=ds;}
        @EventListener(ApplicationReadyEvent.class)
        public void ready(){
            var p=props.getMultiTenancy();
            if(registry.central().queryForObject("select count(*) from platform_admin",Long.class)==0){
                if(p.getOwnerLogin().isBlank()||p.getOwnerPassword().length()<12||p.getOwnerPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                    throw new IllegalStateException("Configure APP_PLATFORM_ADMIN_LOGIN e APP_PLATFORM_ADMIN_PASSWORD (12 a 72 bytes)");
                registry.central().update("insert into platform_admin(login,senha) values (?,?)",p.getOwnerLogin().trim().toLowerCase(java.util.Locale.ROOT),encoder.encode(p.getOwnerPassword()));
            }
            for(var c:registry.all())if("ATIVA".equals(c.estado()))registry.dataSource(c);
            routing.initialized();
        }
    }
}
