package bruninho.Barber.web;

import bruninho.Barber.repository.AppointmentRepository;
import bruninho.Barber.repository.BarberRepository;
import bruninho.Barber.service.BusinessException;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/barbeiro")
public class BarberAreaController {
    private final BarberRepository barbers;
    private final AppointmentRepository appointments;

    public BarberAreaController(BarberRepository barbers, AppointmentRepository appointments) {
        this.barbers = barbers;
        this.appointments = appointments;
    }

    @GetMapping("/agenda")
    String agenda(@RequestParam(required = false) LocalDate data, Authentication auth, Model model) {
        var barber = barbers.findByUserUsername(auth.getName()).orElseThrow(() -> new BusinessException("Barbeiro não encontrado."));
        LocalDate d = data == null ? LocalDate.now() : data;
        model.addAttribute("barbeiro", barber);
        model.addAttribute("data", d);
        model.addAttribute("agendamentos", appointments.findAgenda(barber.getId(), d.atStartOfDay(), d.plusDays(1).atStartOfDay()));
        return "barbeiro/agenda";
    }
}
