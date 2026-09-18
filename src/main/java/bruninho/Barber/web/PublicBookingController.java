package bruninho.Barber.web;

import bruninho.Barber.repository.*;
import bruninho.Barber.service.*;
import bruninho.Barber.web.form.PublicAppointmentForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agendar")
public class PublicBookingController {
    private final ServiceCatalogRepository services;
    private final BarberRepository barbers;
    private final AvailabilityService availability;
    private final AppointmentService appointments;
    private final BarberShopConfigRepository configs;

    public PublicBookingController(ServiceCatalogRepository services, BarberRepository barbers,
                                   AvailabilityService availability, AppointmentService appointments,
                                   BarberShopConfigRepository configs) {
        this.services = services;
        this.barbers = barbers;
        this.availability = availability;
        this.appointments = appointments;
        this.configs = configs;
    }

    @GetMapping
    String form(@ModelAttribute("form") PublicAppointmentForm form, Model model) {
        if (form.getDate() == null) form.setDate(LocalDate.now());
        fill(model, form);
        return "public/agendar";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") PublicAppointmentForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fill(model, form);
            return "public/agendar";
        }
        try {
            var appointment = appointments.createPublic(form);
            redirect.addFlashAttribute("codigo", appointment.getConfirmationCode());
            return "redirect:/agendar/confirmado";
        } catch (BusinessException ex) {
            model.addAttribute("erro", ex.getMessage());
            fill(model, form);
            return "public/agendar";
        }
    }

    @GetMapping("/confirmado")
    String confirmed() { return "public/confirmado"; }

    private void fill(Model model, PublicAppointmentForm form) {
        model.addAttribute("config", configs.findById(1L).orElseThrow());
        model.addAttribute("servicos", services.findByActiveTrueOrderByNome());
        model.addAttribute("barbeiros", form.getServiceId() == null ? barbers.findByActiveTrueOrderByNome() : barbers.findActiveByService(form.getServiceId()));
        if (form.getServiceId() != null && form.getBarberId() != null && form.getDate() != null) {
            model.addAttribute("horarios", availability.availableSlots(form.getServiceId(), form.getBarberId(), form.getDate()));
        }
    }
}
