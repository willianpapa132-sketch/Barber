package bruninho.Barber.web.form;

import bruninho.Barber.domain.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CashForms {
    public static class OpenCashForm {
        @NotNull @DecimalMin("0.00") private BigDecimal initialCash;
        public BigDecimal getInitialCash() { return initialCash; }
        public void setInitialCash(BigDecimal initialCash) { this.initialCash = initialCash; }
    }
    public static class ReceiptForm {
        @NotNull private Long appointmentId;
        @NotNull private PaymentMethod paymentMethod;
        public Long getAppointmentId() { return appointmentId; }
        public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    }
    public static class ManualMovementForm {
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        @NotBlank @Size(max = 255) private String description;
        @Size(max = 80) private String category;
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    public static class CloseCashForm {
        @NotNull @DecimalMin("0.00") private BigDecimal countedCash;
        public BigDecimal getCountedCash() { return countedCash; }
        public void setCountedCash(BigDecimal countedCash) { this.countedCash = countedCash; }
    }
    public static class ReversalForm {
        @NotBlank @Size(max = 255) private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
