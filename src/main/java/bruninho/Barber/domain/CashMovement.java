package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movement")
public class CashMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private CashSession cashSession;
    @ManyToOne
    private Appointment appointment;
    @ManyToOne
    private CashMovement originalMovement;
    @Enumerated(EnumType.STRING)
    private CashMovementType type;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    private String description;
    private String category;
    @ManyToOne(optional = false)
    private AppUser createdByUser;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String reversalReason;
    private boolean reversed;

    public Long getId() { return id; }
    public CashSession getCashSession() { return cashSession; }
    public void setCashSession(CashSession cashSession) { this.cashSession = cashSession; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public CashMovement getOriginalMovement() { return originalMovement; }
    public void setOriginalMovement(CashMovement originalMovement) { this.originalMovement = originalMovement; }
    public CashMovementType getType() { return type; }
    public void setType(CashMovementType type) { this.type = type; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public AppUser getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(AppUser createdByUser) { this.createdByUser = createdByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }
    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }
}
