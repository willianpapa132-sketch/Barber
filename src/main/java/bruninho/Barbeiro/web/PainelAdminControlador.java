package bruninho.Barbeiro.web;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ClienteRepositorio;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.service.AdminCatalogoServico;
import bruninho.Barbeiro.service.AgendamentoServico;
import bruninho.Barbeiro.service.RegraNegocioException;
import bruninho.Barbeiro.web.form.AgendamentoForm;
import bruninho.Barbeiro.web.form.BarbeiroForm;
import bruninho.Barbeiro.web.form.ClienteForm;
import bruninho.Barbeiro.web.form.ServicoForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class PainelAdminControlador {
    private final ServicoRepositorio servicos;
    private final BarbeiroRepositorio barbeiros;
    private final ClienteRepositorio clientes;
    private final AgendamentoRepositorio agendamentos;
    private final JornadaBarbeiroRepositorio jornadas;
    private final AdminCatalogoServico adminCatalogo;
    private final AgendamentoServico agendamentoServico;

    public PainelAdminControlador(ServicoRepositorio servicos, BarbeiroRepositorio barbeiros, ClienteRepositorio clientes,
                           AgendamentoRepositorio agendamentos, JornadaBarbeiroRepositorio jornadas,
                           AdminCatalogoServico adminCatalogo, AgendamentoServico agendamentoServico) {
        this.servicos = servicos;
        this.barbeiros = barbeiros;
        this.clientes = clientes;
        this.agendamentos = agendamentos;
        this.jornadas = jornadas;
        this.adminCatalogo = adminCatalogo;
        this.agendamentoServico = agendamentoServico;
    }

    @GetMapping
    String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        model.addAttribute("agendaHoje", agendamentos.findTop20ByInicioEmBetweenOrderByInicioEm(from, to));
        model.addAttribute("concluidos", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.CONCLUIDO, from, to));
        model.addAttribute("cancelados", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.CANCELADO, from, to));
        model.addAttribute("faltas", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.NAO_COMPARECEU, from, to));
        return "admin/dashboard";
    }

    @GetMapping("/servicos")
    String servicos(Model model) {
        model.addAttribute("servicos", servicos.findAllByOrderByNome());
        model.addAttribute("form", new ServicoForm());
        return "admin/servicos";
    }

    @PostMapping("/servicos")
    String saveService(@Valid @ModelAttribute("form") ServicoForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("servicos", servicos.findAllByOrderByNome());
            return "admin/servicos";
        }
        adminCatalogo.saveService(null, form);
        ra.addFlashAttribute("sucesso", "Serviço salvo.");
        return "redirect:/admin/servicos";
    }

    @PostMapping("/servicos/{id}")
    String updateService(@PathVariable Long id, @Valid @ModelAttribute("form") ServicoForm form, BindingResult result, RedirectAttributes ra) {
        if (!result.hasErrors()) adminCatalogo.saveService(id, form);
        ra.addFlashAttribute("sucesso", "Serviço atualizado.");
        return "redirect:/admin/servicos";
    }

    @GetMapping("/barbeiros")
    String barbeiros(Model model) {
        model.addAttribute("barbeiros", barbeiros.findAllByOrderByNome());
        model.addAttribute("servicos", servicos.findAllByOrderByNome());
        model.addAttribute("form", new BarbeiroForm());
        return "admin/barbeiros";
    }

    @PostMapping("/barbeiros")
    String saveBarbeiro(@Valid @ModelAttribute("form") BarbeiroForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("barbeiros", barbeiros.findAllByOrderByNome());
            model.addAttribute("servicos", servicos.findAllByOrderByNome());
            return "admin/barbeiros";
        }
        try {
            Barbeiro b = adminCatalogo.saveBarbeiro(null, form);
            adminCatalogo.defaultSchedule(b.getId());
            ra.addFlashAttribute("sucesso", "Barbeiro salvo com jornada padrão.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/barbeiros";
    }

    @GetMapping("/clientes")
    String clientes(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("clientes", q.isBlank() ? clientes.findAll() : clientes.findTop30ByNomeContainingIgnoreCaseOrTelefoneNormalizadoContainingOrderByNome(q, q.replaceAll("\\D", "")));
        model.addAttribute("form", new ClienteForm());
        model.addAttribute("q", q);
        return "admin/clientes";
    }

    @PostMapping("/clientes")
    String saveClient(@Valid @ModelAttribute("form") ClienteForm form, BindingResult result, RedirectAttributes ra) {
        try {
            if (!result.hasErrors()) adminCatalogo.saveCliente(null, form);
            ra.addFlashAttribute("sucesso", "Cliente salvo.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    @GetMapping("/agenda")
    String agenda(@RequestParam(required = false) Long barbeiroId,
                  @RequestParam(required = false) StatusAgendamento status,
                  @RequestParam(required = false) LocalDate data,
                  Model model) {
        LocalDate d = data == null ? LocalDate.now() : data;
        model.addAttribute("agendamentos", agendamentos.search(barbeiroId, d.atStartOfDay(), d.plusDays(1).atStartOfDay(), status));
        model.addAttribute("barbeiros", barbeiros.findAllByOrderByNome());
        model.addAttribute("servicos", servicos.findByAtivoTrueOrderByNome());
        model.addAttribute("form", new AgendamentoForm());
        model.addAttribute("data", d);
        return "admin/agenda";
    }

    @PostMapping("/agenda")
    String createAgendamento(@Valid @ModelAttribute("form") AgendamentoForm form, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) throw new RegraNegocioException("Revise os campos do agendamento.");
            agendamentoServico.createManual(form);
            ra.addFlashAttribute("sucesso", "Agendamento criado.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/agenda";
    }

    @PostMapping("/agenda/{id}/status")
    String status(@PathVariable Long id, @RequestParam StatusAgendamento status, @RequestParam(defaultValue = "") String reason, RedirectAttributes ra) {
        try {
            agendamentoServico.changeStatus(id, status, reason);
            ra.addFlashAttribute("sucesso", "Status atualizado.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/agenda";
    }
}
