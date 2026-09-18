package bruninho.Barber.web;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import bruninho.Barber.service.*;
import bruninho.Barber.web.form.CashForms.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/caixa")
public class CashController {
    private final CashSessionRepository sessions;
    private final CashMovementRepository movements;
    private final AppointmentRepository appointments;
    private final CashService cash;

    public CashController(CashSessionRepository sessions, CashMovementRepository movements,
                          AppointmentRepository appointments, CashService cash) {
        this.sessions = sessions;
        this.movements = movements;
        this.appointments = appointments;
        this.cash = cash;
    }

    @GetMapping
    String index(Model model) {
        var open = sessions.findByStatus(CashSessionStatus.ABERTO);
        model.addAttribute("open", open.orElse(null));
        model.addAttribute("movimentos", open.map(s -> movements.findByCashSessionIdOrderByCreatedAt(s.getId())).orElse(java.util.List.of()));
        model.addAttribute("openForm", new OpenCashForm());
        model.addAttribute("receiptForm", new ReceiptForm());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("pendentes", appointments.search(null, java.time.LocalDate.now().minusDays(30).atStartOfDay(), java.time.LocalDate.now().plusDays(1).atStartOfDay(), AppointmentStatus.CONCLUIDO)
            .stream().filter(a -> !a.isPaymentReceived()).toList());
        return "admin/caixa";
    }

    @PostMapping("/abrir")
    String open(@Valid @ModelAttribute OpenCashForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) cash.open(form.getInitialCash()); ra.addFlashAttribute("sucesso", "Caixa aberto."); }
        catch (BusinessException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/receber")
    String receipt(@Valid @ModelAttribute ReceiptForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) cash.receipt(form.getAppointmentId(), form.getPaymentMethod()); ra.addFlashAttribute("sucesso", "Recebimento registrado."); }
        catch (BusinessException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/movimento")
    String movement(@RequestParam CashMovementType type, @Valid @ModelAttribute ManualMovementForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) cash.manual(type, form.getAmount(), form.getDescription(), form.getCategory()); ra.addFlashAttribute("sucesso", "Movimento registrado."); }
        catch (BusinessException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/estornar/{id}")
    String reverse(@PathVariable Long id, @RequestParam String reason, RedirectAttributes ra) {
        try { cash.reverse(id, reason); ra.addFlashAttribute("sucesso", "Estorno registrado."); }
        catch (BusinessException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/fechar")
    String close(@Valid @ModelAttribute CloseCashForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) cash.close(form.getCountedCash()); ra.addFlashAttribute("sucesso", "Caixa fechado."); }
        catch (BusinessException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }
}
