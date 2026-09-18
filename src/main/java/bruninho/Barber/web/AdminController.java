package bruninho.Barber.web;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import bruninho.Barber.service.*;
import bruninho.Barber.web.form.*;
import jakarta.validation.Valid;
import java.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ServiceCatalogRepository services;
    private final BarberRepository barbers;
    private final CustomerRepository customers;
    private final AppointmentRepository appointments;
    private final BarberWorkScheduleRepository schedules;
    private final AdminCatalogService adminCatalog;
    private final AppointmentService appointmentService;

    public AdminController(ServiceCatalogRepository services, BarberRepository barbers, CustomerRepository customers,
                           AppointmentRepository appointments, BarberWorkScheduleRepository schedules,
                           AdminCatalogService adminCatalog, AppointmentService appointmentService) {
        this.services = services;
        this.barbers = barbers;
        this.customers = customers;
        this.appointments = appointments;
        this.schedules = schedules;
        this.adminCatalog = adminCatalog;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        model.addAttribute("agendaHoje", appointments.findTop20ByStartAtBetweenOrderByStartAt(from, to));
        model.addAttribute("concluidos", appointments.countByStatusAndStartAtBetween(AppointmentStatus.CONCLUIDO, from, to));
        model.addAttribute("cancelados", appointments.countByStatusAndStartAtBetween(AppointmentStatus.CANCELADO, from, to));
        model.addAttribute("faltas", appointments.countByStatusAndStartAtBetween(AppointmentStatus.NAO_COMPARECEU, from, to));
        return "admin/dashboard";
    }

    @GetMapping("/servicos")
    String services(Model model) {
        model.addAttribute("servicos", services.findAllByOrderByNome());
        model.addAttribute("form", new ServiceForm());
        return "admin/servicos";
    }

    @PostMapping("/servicos")
    String saveService(@Valid @ModelAttribute("form") ServiceForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("servicos", services.findAllByOrderByNome());
            return "admin/servicos";
        }
        adminCatalog.saveService(null, form);
        ra.addFlashAttribute("sucesso", "Serviço salvo.");
        return "redirect:/admin/servicos";
    }

    @PostMapping("/servicos/{id}")
    String updateService(@PathVariable Long id, @Valid @ModelAttribute("form") ServiceForm form, BindingResult result, RedirectAttributes ra) {
        if (!result.hasErrors()) adminCatalog.saveService(id, form);
        ra.addFlashAttribute("sucesso", "Serviço atualizado.");
        return "redirect:/admin/servicos";
    }

    @GetMapping("/barbeiros")
    String barbers(Model model) {
        model.addAttribute("barbeiros", barbers.findAllByOrderByNome());
        model.addAttribute("servicos", services.findAllByOrderByNome());
        model.addAttribute("form", new BarberForm());
        return "admin/barbeiros";
    }

    @PostMapping("/barbeiros")
    String saveBarber(@Valid @ModelAttribute("form") BarberForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("barbeiros", barbers.findAllByOrderByNome());
            model.addAttribute("servicos", services.findAllByOrderByNome());
            return "admin/barbeiros";
        }
        try {
            Barber b = adminCatalog.saveBarber(null, form);
            adminCatalog.defaultSchedule(b.getId());
            ra.addFlashAttribute("sucesso", "Barbeiro salvo com jornada padrão.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/barbeiros";
    }

    @GetMapping("/clientes")
    String clients(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("clientes", q.isBlank() ? customers.findAll() : customers.findTop30ByNomeContainingIgnoreCaseOrTelefoneNormalizadoContainingOrderByNome(q, q.replaceAll("\\D", "")));
        model.addAttribute("form", new CustomerForm());
        model.addAttribute("q", q);
        return "admin/clientes";
    }

    @PostMapping("/clientes")
    String saveClient(@Valid @ModelAttribute("form") CustomerForm form, BindingResult result, RedirectAttributes ra) {
        try {
            if (!result.hasErrors()) adminCatalog.saveCustomer(null, form);
            ra.addFlashAttribute("sucesso", "Cliente salvo.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    @GetMapping("/agenda")
    String agenda(@RequestParam(required = false) Long barberId,
                  @RequestParam(required = false) AppointmentStatus status,
                  @RequestParam(required = false) LocalDate data,
                  Model model) {
        LocalDate d = data == null ? LocalDate.now() : data;
        model.addAttribute("agendamentos", appointments.search(barberId, d.atStartOfDay(), d.plusDays(1).atStartOfDay(), status));
        model.addAttribute("barbeiros", barbers.findAllByOrderByNome());
        model.addAttribute("servicos", services.findByActiveTrueOrderByNome());
        model.addAttribute("form", new AppointmentForm());
        model.addAttribute("data", d);
        return "admin/agenda";
    }

    @PostMapping("/agenda")
    String createAppointment(@Valid @ModelAttribute("form") AppointmentForm form, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) throw new BusinessException("Revise os campos do agendamento.");
            appointmentService.createManual(form);
            ra.addFlashAttribute("sucesso", "Agendamento criado.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/agenda";
    }

    @PostMapping("/agenda/{id}/status")
    String status(@PathVariable Long id, @RequestParam AppointmentStatus status, @RequestParam(defaultValue = "") String reason, RedirectAttributes ra) {
        try {
            appointmentService.changeStatus(id, status, reason);
            ra.addFlashAttribute("sucesso", "Status atualizado.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/agenda";
    }
}
