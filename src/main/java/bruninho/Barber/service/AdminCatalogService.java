package bruninho.Barber.service;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import bruninho.Barber.web.form.*;
import java.time.LocalTime;
import java.util.HashSet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogService {
    private final ServiceCatalogRepository services;
    private final BarberRepository barbers;
    private final AppUserRepository users;
    private final BarberWorkScheduleRepository schedules;
    private final CustomerRepository customers;
    private final PhoneNormalizer phones;
    private final PasswordEncoder encoder;

    public AdminCatalogService(ServiceCatalogRepository services, BarberRepository barbers, AppUserRepository users,
                               BarberWorkScheduleRepository schedules, CustomerRepository customers,
                               PhoneNormalizer phones, PasswordEncoder encoder) {
        this.services = services;
        this.barbers = barbers;
        this.users = users;
        this.schedules = schedules;
        this.customers = customers;
        this.phones = phones;
        this.encoder = encoder;
    }

    @Transactional
    public ServiceCatalog saveService(Long id, ServiceForm form) {
        ServiceCatalog service = id == null ? new ServiceCatalog() : services.findById(id).orElseThrow();
        service.setNome(form.getNome().trim());
        service.setDescricao(form.getDescricao());
        service.setPreco(form.getPreco());
        service.setDuracaoMinutos(form.getDuracaoMinutos());
        service.setActive(form.isActive());
        service.touch();
        return services.save(service);
    }

    @Transactional
    public Barber saveBarber(Long id, BarberForm form) {
        phones.validate(form.getTelefone());
        Barber barber = id == null ? new Barber() : barbers.findById(id).orElseThrow();
        AppUser user = id == null ? new AppUser() : barber.getUser();
        if (id == null && users.existsByUsername(form.getUsername())) throw new BusinessException("Usuário já existe.");
        user.setUsername(form.getUsername().trim());
        user.setNome(form.getNome().trim());
        user.setRole(Role.BARBEIRO);
        user.setActive(form.isActive());
        if (id == null || (form.getPassword() != null && !form.getPassword().isBlank())) {
            user.setPasswordHash(encoder.encode(form.getPassword()));
        }
        user.touch();
        users.save(user);
        barber.setNome(form.getNome().trim());
        barber.setTelefone(form.getTelefone());
        barber.setActive(form.isActive());
        barber.setUser(user);
        barber.getServices().clear();
        barber.getServices().addAll(new HashSet<>(services.findAllById(form.getServiceIds())));
        return barbers.save(barber);
    }

    @Transactional
    public void defaultSchedule(Long barberId) {
        Barber barber = barbers.findById(barberId).orElseThrow();
        schedules.deleteByBarberId(barberId);
        for (int d = 1; d <= 6; d++) {
            BarberWorkSchedule schedule = new BarberWorkSchedule();
            schedule.setBarber(barber);
            schedule.setDayOfWeek(d);
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(d == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            schedule.setBreakStart(LocalTime.of(12, 0));
            schedule.setBreakEnd(LocalTime.of(13, 0));
            schedules.save(schedule);
        }
    }

    @Transactional
    public Customer saveCustomer(Long id, CustomerForm form) {
        phones.validate(form.getTelefone());
        Customer c = id == null ? new Customer() : customers.findById(id).orElseThrow();
        c.setNome(form.getNome().trim());
        c.setTelefone(form.getTelefone().trim());
        c.setTelefoneNormalizado(phones.normalize(form.getTelefone()));
        c.touch();
        return customers.save(c);
    }
}
