package bruninho.Barber.config;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DataInitializer {
    @Bean
    CommandLineRunner bootstrap(AppProperties props, AppUserRepository users, PasswordEncoder encoder,
                                ServiceCatalogRepository services, BarberRepository barbers,
                                BarberWorkScheduleRepository schedules) {
        return args -> run(props, users, encoder, services, barbers, schedules);
    }

    @Transactional
    void run(AppProperties props, AppUserRepository users, PasswordEncoder encoder,
             ServiceCatalogRepository services, BarberRepository barbers,
             BarberWorkScheduleRepository schedules) {
        var admin = props.getInitialAdmin();
        if (!admin.getUsername().isBlank() && !users.existsByUsername(admin.getUsername())) {
            AppUser user = new AppUser();
            user.setUsername(admin.getUsername());
            user.setPasswordHash(encoder.encode(admin.getPassword()));
            user.setNome(admin.getName());
            user.setRole(Role.ADMIN);
            users.save(user);
        }
        if (props.isDemoData() && services.count() == 0) {
            ServiceCatalog corte = new ServiceCatalog();
            corte.setNome("Corte masculino");
            corte.setDescricao("Corte com acabamento");
            corte.setPreco(new BigDecimal("45.00"));
            corte.setDuracaoMinutos(45);
            services.save(corte);

            AppUser user = new AppUser();
            user.setUsername("barbeiro");
            user.setPasswordHash(encoder.encode("barbeiro123"));
            user.setNome("Barbeiro Demo");
            user.setRole(Role.BARBEIRO);
            users.save(user);

            Barber barber = new Barber();
            barber.setNome("Barbeiro Demo");
            barber.setTelefone("(11) 99999-0000");
            barber.setUser(user);
            barber.getServices().add(corte);
            barbers.save(barber);

            for (int d = 1; d <= 6; d++) {
                BarberWorkSchedule s = new BarberWorkSchedule();
                s.setBarber(barber);
                s.setDayOfWeek(d);
                s.setStartTime(LocalTime.of(9, 0));
                s.setEndTime(d == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
                s.setBreakStart(LocalTime.of(12, 0));
                s.setBreakEnd(LocalTime.of(13, 0));
                schedules.save(s);
            }
        }
    }
}
