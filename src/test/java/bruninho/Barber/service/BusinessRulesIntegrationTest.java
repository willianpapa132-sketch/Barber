package bruninho.Barber.service;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import bruninho.Barber.web.form.PublicAppointmentForm;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BusinessRulesIntegrationTest {
    @Autowired BarberShopConfigRepository configs;
    @Autowired ShopHoursRepository shopHours;
    @Autowired ServiceCatalogRepository services;
    @Autowired AppUserRepository users;
    @Autowired BarberRepository barbers;
    @Autowired BarberWorkScheduleRepository schedules;
    @Autowired BarberBlockRepository blocks;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentService appointmentService;
    @Autowired AvailabilityService availability;
    @Autowired CashService cashService;
    @Autowired CashMovementRepository movements;
    @Autowired CashSessionRepository sessions;

    ServiceCatalog service;
    Barber barber;
    LocalDate nextMonday;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        users.findByUsername("admin").orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPasswordHash("hash");
            admin.setNome("Admin");
            admin.setRole(Role.ADMIN);
            return users.save(admin);
        });
        movements.deleteAll();
        sessions.deleteAll();
        appointments.deleteAll();
        blocks.deleteAll();
        schedules.deleteAll();
        barbers.deleteAll();
        services.deleteAll();
        configs.deleteAll();
        BarberShopConfig config = new BarberShopConfig();
        config.setNome("Barbearia Teste");
        config.setMinAntecedenciaMinutos(0);
        config.setHorizonteDias(60);
        configs.save(config);
        shopHours.deleteAll();
        for (int d = 1; d <= 7; d++) {
            ShopHours h = new ShopHours();
            h.setDayOfWeek(d);
            h.setOpenTime(LocalTime.of(9, 0));
            h.setCloseTime(LocalTime.of(18, 0));
            h.setClosed(false);
            shopHours.save(h);
        }
        service = new ServiceCatalog();
        service.setNome("Corte");
        service.setPreco(new BigDecimal("50.00"));
        service.setDuracaoMinutos(45);
        service = services.save(service);

        AppUser user = new AppUser();
        user.setUsername("barbeiro-rule-" + System.nanoTime());
        user.setPasswordHash("hash");
        user.setNome("Barbeiro");
        user.setRole(Role.BARBEIRO);
        users.save(user);
        barber = new Barber();
        barber.setNome("Barbeiro");
        barber.setUser(user);
        barber.getServices().add(service);
        barber = barbers.save(barber);

        BarberWorkSchedule schedule = new BarberWorkSchedule();
        schedule.setBarber(barber);
        schedule.setDayOfWeek(1);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setBreakStart(LocalTime.of(12, 0));
        schedule.setBreakEnd(LocalTime.of(13, 0));
        schedules.save(schedule);

        int daysUntilMonday = (8 - LocalDate.now().getDayOfWeek().getValue()) % 7;
        nextMonday = LocalDate.now().plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
    }

    @Test
    void disponibilidadeRespeitaDuracaoJornadaIntervaloBloqueioESobreposicao() {
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).contains(LocalTime.of(9, 0));
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).doesNotContain(LocalTime.of(11, 30), LocalTime.of(16, 30));

        BarberBlock block = new BarberBlock();
        block.setBarber(barber);
        block.setBlockDate(nextMonday);
        block.setStartTime(LocalTime.of(10, 0));
        block.setEndTime(LocalTime.of(11, 0));
        blocks.save(block);
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).doesNotContain(LocalTime.of(10, 0), LocalTime.of(10, 15));
    }

    @Test
    void rejeitaSobreposicaoPreservaPrecoECancelamentoLiberaHorario() {
        var first = appointmentService.createPublic(form(LocalTime.of(9, 0)));
        service.setPreco(new BigDecimal("80.00"));
        services.save(service);
        assertThat(first.getServicePriceSnapshot()).isEqualByComparingTo("50.00");

        assertThatThrownBy(() -> appointmentService.createPublic(form(LocalTime.of(9, 15))))
            .isInstanceOf(BusinessException.class);

        appointmentService.changeStatus(first.getId(), AppointmentStatus.CANCELADO, "cliente pediu");
        assertThatCode(() -> appointmentService.createPublic(form(LocalTime.of(9, 0)))).doesNotThrowAnyException();
    }

    @Test
    void agendarEConcluirNaoGeramRecebimentoAutomatico() {
        var appt = appointmentService.createPublic(form(LocalTime.of(9, 0)));
        appointmentService.changeStatus(appt.getId(), AppointmentStatus.CONCLUIDO, "");
        assertThat(movements.findAll()).isEmpty();
        assertThat(appointments.findById(appt.getId()).orElseThrow().isPaymentReceived()).isFalse();
    }

    @Test
    void caixaImpedeDuplicidadeEstornaECalculaDinheiroFisico() {
        var cash = cashService.open(new BigDecimal("100.00"));
        var appt = appointmentService.createPublic(form(LocalTime.of(9, 0)));
        appointmentService.changeStatus(appt.getId(), AppointmentStatus.CONCLUIDO, "");

        var pix = cashService.receipt(appt.getId(), PaymentMethod.PIX);
        assertThat(cashService.expectedCash(cash.getId(), cash.getInitialCash())).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> cashService.receipt(appt.getId(), PaymentMethod.PIX)).isInstanceOf(BusinessException.class);

        cashService.reverse(pix.getId(), "erro");
        assertThat(appointments.findById(appt.getId()).orElseThrow().isPaymentReceived()).isFalse();
        cashService.receipt(appt.getId(), PaymentMethod.DINHEIRO);
        assertThat(cashService.expectedCash(cash.getId(), cash.getInitialCash())).isEqualByComparingTo("150.00");

        cashService.close(new BigDecimal("150.00"));
        assertThatThrownBy(() -> cashService.manual(CashMovementType.SAIDA_MANUAL, BigDecimal.TEN, "despesa", "geral"))
            .isInstanceOf(BusinessException.class);
    }

    private PublicAppointmentForm form(LocalTime time) {
        PublicAppointmentForm form = new PublicAppointmentForm();
        form.setServiceId(service.getId());
        form.setBarberId(barber.getId());
        form.setDate(nextMonday);
        form.setTime(time);
        form.setCustomerName("Cliente");
        form.setCustomerPhone("(11) 99999-9999");
        return form;
    }
}
