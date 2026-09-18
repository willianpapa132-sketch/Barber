package bruninho.Barber.config;

import bruninho.Barber.domain.AppUser;
import bruninho.Barber.domain.Barber;
import bruninho.Barber.domain.BarberShopConfig;
import bruninho.Barber.domain.BarberWorkSchedule;
import bruninho.Barber.domain.Role;
import bruninho.Barber.domain.ServiceCatalog;
import bruninho.Barber.domain.ShopHours;
import bruninho.Barber.repository.AppUserRepository;
import bruninho.Barber.repository.BarberRepository;
import bruninho.Barber.repository.BarberShopConfigRepository;
import bruninho.Barber.repository.BarberWorkScheduleRepository;
import bruninho.Barber.repository.ServiceCatalogRepository;
import bruninho.Barber.repository.ShopHoursRepository;
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
                                BarberWorkScheduleRepository schedules,
                                BarberShopConfigRepository configs,
                                ShopHoursRepository shopHours) {
        return args -> run(props, users, encoder, services, barbers, schedules, configs, shopHours);
    }

    @Transactional
    void run(AppProperties props, AppUserRepository users, PasswordEncoder encoder,
             ServiceCatalogRepository services, BarberRepository barbers,
             BarberWorkScheduleRepository schedules,
             BarberShopConfigRepository configs,
             ShopHoursRepository shopHours) {
        ensureConfig(configs);
        ensureShopHours(shopHours);
        ensureInitialAdmin(props, users, encoder);
        if (props.isDemoData() && services.count() == 0) {
            createDemoData(users, encoder, services, barbers, schedules);
        }
    }

    private void ensureConfig(BarberShopConfigRepository configs) {
        configs.findById(1L).orElseGet(() -> {
            BarberShopConfig config = new BarberShopConfig();
            config.setNome("Barbearia");
            config.setTelefone("");
            config.setEndereco("");
            config.setMinAntecedenciaMinutos(30);
            config.setHorizonteDias(60);
            return configs.save(config);
        });
    }

    private void ensureShopHours(ShopHoursRepository shopHours) {
        for (int day = 1; day <= 7; day++) {
            final int currentDay = day;
            shopHours.findByDayOfWeek(currentDay).orElseGet(() -> {
                ShopHours hours = new ShopHours();
                hours.setDayOfWeek(currentDay);
                hours.setOpenTime(LocalTime.of(9, 0));
                hours.setCloseTime(currentDay == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
                hours.setClosed(currentDay == 7);
                return shopHours.save(hours);
            });
        }
    }

    private void ensureInitialAdmin(AppProperties props, AppUserRepository users, PasswordEncoder encoder) {
        var admin = props.getInitialAdmin();
        if (!admin.getUsername().isBlank() && !users.existsByUsername(admin.getUsername())) {
            AppUser user = new AppUser();
            user.setUsername(admin.getUsername());
            user.setPasswordHash(encoder.encode(admin.getPassword()));
            user.setNome(admin.getName());
            user.setRole(Role.ADMIN);
            users.save(user);
        }
    }

    private void createDemoData(AppUserRepository users, PasswordEncoder encoder,
                                ServiceCatalogRepository services, BarberRepository barbers,
                                BarberWorkScheduleRepository schedules) {
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

        for (int day = 1; day <= 6; day++) {
            BarberWorkSchedule schedule = new BarberWorkSchedule();
            schedule.setBarber(barber);
            schedule.setDayOfWeek(day);
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(day == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            schedule.setBreakStart(LocalTime.of(12, 0));
            schedule.setBreakEnd(LocalTime.of(13, 0));
            schedules.save(schedule);
        }
    }
}
