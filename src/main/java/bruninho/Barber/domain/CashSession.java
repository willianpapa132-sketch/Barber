package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_session")
public class CashSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime openedAt = LocalDateTime.now();
    private LocalDateTime closedAt;
    @ManyToOne(optional = false)
    private AppUser openedByUser;
    @ManyToOne
    private AppUser closedByUser;
    @Column(precision = 12, scale = 2)
    private BigDecimal initialCash;
    @Column(precision = 12, scale = 2)
    private BigDecimal expectedCash;
    @Column(precision = 12, scale = 2)
    private BigDecimal countedCash;
    @Column(precision = 12, scale = 2)
    private BigDecimal differenceCash;
    @Enumerated(EnumType.STRING)
    private CashSessionStatus status = CashSessionStatus.ABERTO;

    public Long getId() { return id; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public AppUser getOpenedByUser() { return openedByUser; }
    public void setOpenedByUser(AppUser openedByUser) { this.openedByUser = openedByUser; }
    public AppUser getClosedByUser() { return closedByUser; }
    public void setClosedByUser(AppUser closedByUser) { this.closedByUser = closedByUser; }
    public BigDecimal getInitialCash() { return initialCash; }
    public void setInitialCash(BigDecimal initialCash) { this.initialCash = initialCash; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }
    public BigDecimal getCountedCash() { return countedCash; }
    public void setCountedCash(BigDecimal countedCash) { this.countedCash = countedCash; }
    public BigDecimal getDifferenceCash() { return differenceCash; }
    public void setDifferenceCash(BigDecimal differenceCash) { this.differenceCash = differenceCash; }
    public CashSessionStatus getStatus() { return status; }
    public void setStatus(CashSessionStatus status) { this.status = status; }
}
