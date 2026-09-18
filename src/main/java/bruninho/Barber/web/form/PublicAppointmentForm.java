package bruninho.Barber.web.form;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class PublicAppointmentForm {
    @NotNull private Long serviceId;
    @NotNull private Long barberId;
    @NotNull private LocalDate date;
    @NotNull private LocalTime time;
    @NotBlank @Size(max = 120) private String customerName;
    @NotBlank @Size(max = 30) private String customerPhone;

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getBarberId() { return barberId; }
    public void setBarberId(Long barberId) { this.barberId = barberId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
}
