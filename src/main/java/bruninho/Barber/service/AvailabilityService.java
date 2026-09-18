package bruninho.Barber.service;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {
    private static final List<AppointmentStatus> BLOCKING = List.of(AppointmentStatus.AGENDADO, AppointmentStatus.CONCLUIDO);
    private final BarberRepository barbers;
    private final ServiceCatalogRepository services;
    private final BarberWorkScheduleRepository schedules;
    private final BarberBlockRepository blocks;
    private final AppointmentRepository appointments;
    private final BarberShopConfigRepository configs;
    private final ShopHoursRepository shopHours;
    private final ZoneId zoneId;

    public AvailabilityService(BarberRepository barbers, ServiceCatalogRepository services,
                               BarberWorkScheduleRepository schedules, BarberBlockRepository blocks,
                               AppointmentRepository appointments, BarberShopConfigRepository configs,
                               ShopHoursRepository shopHours, @Value("${app.zone}") String zone) {
        this.barbers = barbers;
        this.services = services;
        this.schedules = schedules;
        this.blocks = blocks;
        this.appointments = appointments;
        this.configs = configs;
        this.shopHours = shopHours;
        this.zoneId = ZoneId.of(zone);
    }

    public List<LocalTime> availableSlots(Long serviceId, Long barberId, LocalDate date) {
        ServiceCatalog service = services.findById(serviceId).orElseThrow();
        List<LocalTime> slots = new ArrayList<>();
        int dow = date.getDayOfWeek().getValue();
        BarberWorkSchedule schedule = scheduleFor(barberId, dow);
        if (schedule == null) return slots;
        LocalTime start = schedule.getStartTime();
        while (!start.plusMinutes(service.getDuracaoMinutos()).isAfter(schedule.getEndTime())) {
            if (isAvailable(service, barberId, LocalDateTime.of(date, start), false, null, schedule)) {
                slots.add(start);
            }
            start = start.plusMinutes(15);
        }
        return slots;
    }

    public void assertAvailable(Long serviceId, Long barberId, LocalDateTime startAt, Long ignoringAppointmentId) {
        ServiceCatalog service = services.findById(serviceId).orElseThrow(() -> new BusinessException("Serviço não encontrado."));
        if (!isAvailable(service, barberId, startAt, true, ignoringAppointmentId)) {
            throw new BusinessException("Horário indisponível para o serviço e profissional selecionados.");
        }
    }

    public boolean isAvailable(ServiceCatalog service, Long barberId, LocalDateTime startAt, boolean strictPast) {
        return isAvailable(service, barberId, startAt, strictPast, null, null);
    }

    private boolean isAvailable(ServiceCatalog service, Long barberId, LocalDateTime startAt, boolean strictPast, Long ignoringAppointmentId) {
        return isAvailable(service, barberId, startAt, strictPast, ignoringAppointmentId, null);
    }

    private boolean isAvailable(ServiceCatalog service, Long barberId, LocalDateTime startAt, boolean strictPast, Long ignoringAppointmentId, BarberWorkSchedule prefetchedSchedule) {
        if (service == null || !service.isActive()) return false;
        Barber barber = barbers.findById(barberId).orElse(null);
        if (barber == null || !barber.isActive()) return false;
        boolean performs = barber.getServices().stream().anyMatch(s -> Objects.equals(s.getId(), service.getId()) && s.isActive());
        if (!performs) return false;

        BarberShopConfig config = configs.findById(1L).orElseThrow();
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDate date = startAt.toLocalDate();
        if (strictPast && startAt.isBefore(now.plusMinutes(config.getMinAntecedenciaMinutos()))) return false;
        if (date.isAfter(now.toLocalDate().plusDays(config.getHorizonteDias()))) return false;
        if (startAt.getMinute() % 15 != 0 || startAt.getSecond() != 0 || startAt.getNano() != 0) return false;

        LocalDateTime endAt = startAt.plusMinutes(service.getDuracaoMinutos());
        int dow = date.getDayOfWeek().getValue();
        ShopHours hours = shopHours.findByDayOfWeek(dow).orElse(null);
        if (hours == null || hours.isClosed()) return false;
        if (startAt.toLocalTime().isBefore(hours.getOpenTime()) || endAt.toLocalTime().isAfter(hours.getCloseTime())) return false;

        BarberWorkSchedule schedule = prefetchedSchedule != null ? prefetchedSchedule : scheduleFor(barberId, dow);
        if (schedule == null) return false;
        if (startAt.toLocalTime().isBefore(schedule.getStartTime()) || endAt.toLocalTime().isAfter(schedule.getEndTime())) return false;
        if (schedule.getBreakStart() != null && schedule.getBreakEnd() != null && overlaps(startAt.toLocalTime(), endAt.toLocalTime(), schedule.getBreakStart(), schedule.getBreakEnd())) return false;

        for (BarberBlock block : blocks.findByBarberIdAndBlockDate(barberId, date)) {
            LocalTime bs = block.getStartTime() == null ? LocalTime.MIN : block.getStartTime();
            LocalTime be = block.getEndTime() == null ? LocalTime.MAX : block.getEndTime();
            if (overlaps(startAt.toLocalTime(), endAt.toLocalTime(), bs, be)) return false;
        }

        return appointments.findConflicts(barberId, startAt, endAt, BLOCKING).stream()
            .allMatch(a -> Objects.equals(a.getId(), ignoringAppointmentId));
    }

    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    private BarberWorkSchedule scheduleFor(Long barberId, int dow) {
        return schedules.findByBarberIdOrderByDayOfWeek(barberId).stream()
            .filter(s -> s.isActive() && s.getDayOfWeek() == dow)
            .findFirst()
            .orElse(null);
    }
}
