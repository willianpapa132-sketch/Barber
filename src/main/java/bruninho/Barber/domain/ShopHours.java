package bruninho.Barber.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "shop_hours")
public class ShopHours {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean closed;

    public Long getId() { return id; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
}
