package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "barber_work_schedule")
public class BarberWorkSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Barber barber;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    private boolean active = true;

    public Long getId() { return id; }
    public Barber getBarber() { return barber; }
    public void setBarber(Barber barber) { this.barber = barber; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public LocalTime getBreakStart() { return breakStart; }
    public void setBreakStart(LocalTime breakStart) { this.breakStart = breakStart; }
    public LocalTime getBreakEnd() { return breakEnd; }
    public void setBreakEnd(LocalTime breakEnd) { this.breakEnd = breakEnd; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
