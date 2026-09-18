package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String confirmationCode;
    @ManyToOne
    private Customer customer;
    @ManyToOne(optional = false)
    private Barber barber;
    @ManyToOne(optional = false)
    private ServiceCatalog service;
    private String customerName;
    private String customerPhone;
    private String serviceNameSnapshot;
    @Column(precision = 12, scale = 2)
    private BigDecimal servicePriceSnapshot;
    private int serviceDurationMinutesSnapshot;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.AGENDADO;
    private boolean paymentReceived;
    @ManyToOne
    private AppUser createdByUser;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private String cancelledReason;

    public Long getId() { return id; }
    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Barber getBarber() { return barber; }
    public void setBarber(Barber barber) { this.barber = barber; }
    public ServiceCatalog getService() { return service; }
    public void setService(ServiceCatalog service) { this.service = service; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getServiceNameSnapshot() { return serviceNameSnapshot; }
    public void setServiceNameSnapshot(String serviceNameSnapshot) { this.serviceNameSnapshot = serviceNameSnapshot; }
    public BigDecimal getServicePriceSnapshot() { return servicePriceSnapshot; }
    public void setServicePriceSnapshot(BigDecimal servicePriceSnapshot) { this.servicePriceSnapshot = servicePriceSnapshot; }
    public int getServiceDurationMinutesSnapshot() { return serviceDurationMinutesSnapshot; }
    public void setServiceDurationMinutesSnapshot(int serviceDurationMinutesSnapshot) { this.serviceDurationMinutesSnapshot = serviceDurationMinutesSnapshot; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public boolean isPaymentReceived() { return paymentReceived; }
    public void setPaymentReceived(boolean paymentReceived) { this.paymentReceived = paymentReceived; }
    public AppUser getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(AppUser createdByUser) { this.createdByUser = createdByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = LocalDateTime.now(); }
    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }
}
