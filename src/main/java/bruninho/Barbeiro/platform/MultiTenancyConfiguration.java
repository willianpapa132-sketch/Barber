package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.sql.DataSource;

@Configuration
public class MultiTenancyConfiguration {
    @Bean TenantRegistry tenantRegistry(DataSource dataSource) { return new TenantRegistry(dataSource); }

    @Bean
    @ConditionalOnProperty(name = "app.multi-tenancy.enabled", havingValue = "true", matchIfMissing = true)
    org.springframework.boot.web.servlet.FilterRegistrationBean<TenantRequestFilter> noServletRegistration(TenantRequestFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "app.multi-tenancy.enabled", havingValue = "true", matchIfMissing = true)
    PlatformBootstrap platformBootstrap(TenantRegistry registry, AppProperties props, PasswordEncoder encoder) {
        return new PlatformBootstrap(registry, props, encoder);
    }

    public static class PlatformBootstrap {
        private final TenantRegistry registry;
        private final AppProperties props;
        private final PasswordEncoder encoder;
        PlatformBootstrap(TenantRegistry registry, AppProperties props, PasswordEncoder encoder) {
            this.registry = registry; this.props = props; this.encoder = encoder;
        }
        @EventListener(ApplicationReadyEvent.class)
        public void ready() {
            var p = props.getMultiTenancy();
            if (registry.central().queryForObject("select count(*) from platform_admin", Long.class) == 0) {
                if (p.getOwnerLogin().isBlank() || p.getOwnerPassword().length() < 12
                        || p.getOwnerPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                    throw new IllegalStateException("Configure APP_PLATFORM_ADMIN_LOGIN e APP_PLATFORM_ADMIN_PASSWORD (12 a 72 bytes)");
                registry.central().update("insert into platform_admin(login,senha) values (?,?)",
                        p.getOwnerLogin().trim().toLowerCase(java.util.Locale.ROOT), encoder.encode(p.getOwnerPassword()));
            }
        }
    }
}
