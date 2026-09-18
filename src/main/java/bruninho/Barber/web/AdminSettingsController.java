package bruninho.Barber.web;

import bruninho.Barber.domain.*;
import bruninho.Barber.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminSettingsController {
    private final BarberShopConfigRepository configs;
    private final CashMovementRepository movements;
    private final AppointmentRepository appointments;

    public AdminSettingsController(BarberShopConfigRepository configs, CashMovementRepository movements, AppointmentRepository appointments) {
        this.configs = configs;
        this.movements = movements;
        this.appointments = appointments;
    }

    @GetMapping("/configuracoes")
    String settings(Model model) {
        model.addAttribute("config", configs.findById(1L).orElseThrow());
        return "admin/configuracoes";
    }

    @PostMapping("/configuracoes")
    String save(@RequestParam String nome, @RequestParam String telefone, @RequestParam String endereco,
                @RequestParam int minAntecedenciaMinutos, @RequestParam int horizonteDias, RedirectAttributes ra) {
        BarberShopConfig config = configs.findById(1L).orElseThrow();
        config.setNome(nome);
        config.setTelefone(telefone);
        config.setEndereco(endereco);
        config.setMinAntecedenciaMinutos(Math.max(0, minAntecedenciaMinutos));
        config.setHorizonteDias(Math.max(1, horizonteDias));
        configs.save(config);
        ra.addFlashAttribute("sucesso", "Configurações salvas.");
        return "redirect:/admin/configuracoes";
    }

    @GetMapping("/relatorios")
    String reports(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim, Model model) {
        LocalDate from = inicio == null ? LocalDate.now().withDayOfMonth(1) : inicio;
        LocalDate to = fim == null ? LocalDate.now() : fim;
        var cash = movements.findByCreatedAtBetweenOrderByCreatedAtDesc(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        BigDecimal receita = cash.stream()
            .filter(m -> !m.isReversed() && (m.getType() == CashMovementType.RECEBIMENTO_SERVICO || m.getType() == CashMovementType.ESTORNO))
            .map(CashMovement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal despesas = cash.stream()
            .filter(m -> !m.isReversed() && m.getType() == CashMovementType.SAIDA_MANUAL)
            .map(CashMovement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("inicio", from);
        model.addAttribute("fim", to);
        model.addAttribute("receita", receita);
        model.addAttribute("despesas", despesas);
        model.addAttribute("movimentos", cash);
        model.addAttribute("concluidos", appointments.countByStatusAndStartAtBetween(AppointmentStatus.CONCLUIDO, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        model.addAttribute("cancelados", appointments.countByStatusAndStartAtBetween(AppointmentStatus.CANCELADO, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        model.addAttribute("faltas", appointments.countByStatusAndStartAtBetween(AppointmentStatus.NAO_COMPARECEU, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        return "admin/relatorios";
    }
}
