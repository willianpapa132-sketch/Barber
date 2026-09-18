package bruninho.Barber.service;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import bruninho.Barber.web.form.AppointmentForm;
import bruninho.Barber.web.form.PublicAppointmentForm;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final BarberRepository barbers;
    private final ServiceCatalogRepository services;
    private final CustomerRepository customers;
    private final AvailabilityService availability;
    private final PhoneNormalizer phoneNormalizer;
    private final CurrentUserService currentUser;
    private final SecureRandom random = new SecureRandom();

    public AppointmentService(AppointmentRepository appointments, BarberRepository barbers, ServiceCatalogRepository services,
                              CustomerRepository customers, AvailabilityService availability, PhoneNormalizer phoneNormalizer,
                              CurrentUserService currentUser) {
        this.appointments = appointments;
        this.barbers = barbers;
        this.services = services;
        this.customers = customers;
        this.availability = availability;
        this.phoneNormalizer = phoneNormalizer;
        this.currentUser = currentUser;
    }

    @Transactional
    public Appointment createPublic(PublicAppointmentForm form) {
        phoneNormalizer.validate(form.getCustomerPhone());
        return create(form.getServiceId(), form.getBarberId(), LocalDateTime.of(form.getDate(), form.getTime()),
            form.getCustomerName(), form.getCustomerPhone(), null, null);
    }

    @Transactional
    public Appointment createManual(AppointmentForm form) {
        phoneNormalizer.validate(form.getCustomerPhone());
        Customer customer = null;
        if (form.getCustomerId() != null) {
            customer = customers.findById(form.getCustomerId()).orElseThrow(() -> new BusinessException("Cliente não encontrado."));
        }
        return create(form.getServiceId(), form.getBarberId(), LocalDateTime.of(form.getDate(), form.getTime()),
            form.getCustomerName(), form.getCustomerPhone(), customer, currentUser.requiredUser());
    }

    private Appointment create(Long serviceId, Long barberId, LocalDateTime startAt, String customerName, String customerPhone, Customer customer, AppUser actor) {
        Barber barber = barbers.lockById(barberId).orElseThrow(() -> new BusinessException("Barbeiro não encontrado."));
        ServiceCatalog service = services.findById(serviceId).orElseThrow(() -> new BusinessException("Serviço não encontrado."));
        availability.assertAvailable(serviceId, barber.getId(), startAt, null);
        Appointment appointment = new Appointment();
        appointment.setConfirmationCode(newCode());
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setCustomer(customer);
        appointment.setCustomerName(customerName.trim());
        appointment.setCustomerPhone(customerPhone.trim());
        appointment.setServiceNameSnapshot(service.getNome());
        appointment.setServicePriceSnapshot(service.getPreco());
        appointment.setServiceDurationMinutesSnapshot(service.getDuracaoMinutos());
        appointment.setStartAt(startAt);
        appointment.setEndAt(startAt.plusMinutes(service.getDuracaoMinutos()));
        appointment.setCreatedByUser(actor);
        return appointments.save(appointment);
    }

    @Transactional
    public Appointment reschedule(Long appointmentId, Long barberId, Long serviceId, LocalDateTime startAt) {
        Appointment appointment = appointments.findById(appointmentId).orElseThrow();
        if (appointment.getStatus() != AppointmentStatus.AGENDADO) {
            throw new BusinessException("Somente agendamentos em aberto podem ser remarcados.");
        }
        Barber barber = barbers.lockById(barberId).orElseThrow();
        ServiceCatalog service = services.findById(serviceId).orElseThrow();
        availability.assertAvailable(serviceId, barberId, startAt, appointmentId);
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setServiceNameSnapshot(service.getNome());
        appointment.setServicePriceSnapshot(service.getPreco());
        appointment.setServiceDurationMinutesSnapshot(service.getDuracaoMinutos());
        appointment.setStartAt(startAt);
        appointment.setEndAt(startAt.plusMinutes(service.getDuracaoMinutos()));
        appointment.touch();
        return appointment;
    }

    @Transactional
    public void changeStatus(Long id, AppointmentStatus target, String reason) {
        Appointment appointment = appointments.findById(id).orElseThrow();
        AppointmentStatus current = appointment.getStatus();
        boolean allowed = current == AppointmentStatus.AGENDADO &&
            (target == AppointmentStatus.CONCLUIDO || target == AppointmentStatus.CANCELADO || target == AppointmentStatus.NAO_COMPARECEU);
        if (!allowed) throw new BusinessException("Transição de status não permitida.");
        appointment.setStatus(target);
        appointment.setCancelledReason(reason);
        appointment.touch();
    }

    private String newCode() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
