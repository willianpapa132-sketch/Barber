package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "barber_block")
public class BarberBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Barber barber;
    private LocalDate blockDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Barber getBarber() { return barber; }
    public void setBarber(Barber barber) { this.barber = barber; }
    public LocalDate getBlockDate() { return blockDate; }
    public void setBlockDate(LocalDate blockDate) { this.blockDate = blockDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
