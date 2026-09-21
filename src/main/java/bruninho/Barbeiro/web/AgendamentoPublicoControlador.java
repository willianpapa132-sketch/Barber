package bruninho.Barbeiro.web;

import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.service.AgendamentoServico;
import bruninho.Barbeiro.service.DisponibilidadeServico;
import bruninho.Barbeiro.service.RegraNegocioException;
import bruninho.Barbeiro.web.form.AgendamentoPublicoForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agendar")
public class AgendamentoPublicoControlador {
    private final ServicoRepositorio servicos;
    private final BarbeiroRepositorio barbeiros;
    private final DisponibilidadeServico disponibilidade;
    private final AgendamentoServico agendamentos;
    private final ConfiguracaoBarbeariaRepositorio configuracoes;

    public AgendamentoPublicoControlador(ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
                                         DisponibilidadeServico disponibilidade, AgendamentoServico agendamentos,
                                         ConfiguracaoBarbeariaRepositorio configuracoes) {
        this.servicos = servicos;
        this.barbeiros = barbeiros;
        this.disponibilidade = disponibilidade;
        this.agendamentos = agendamentos;
        this.configuracoes = configuracoes;
    }

    @GetMapping
    String form(@ModelAttribute("form") AgendamentoPublicoForm form, Model model) {
        if (form.getData() == null) form.setData(LocalDate.now());
        fill(model, form);
        return "public/agendar";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") AgendamentoPublicoForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fill(model, form);
            return "public/agendar";
        }
        try {
            var agendamento = agendamentos.createPublic(form);
            redirect.addFlashAttribute("codigo", agendamento.getCodigoConfirmacao());
            return "redirect:/agendar/confirmado";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erro", ex.getMessage());
            fill(model, form);
            return "public/agendar";
        }
    }

    @GetMapping("/confirmado")
    String confirmed() {
        return "public/confirmado";
    }

    private void fill(Model model, AgendamentoPublicoForm form) {
        model.addAttribute("config", configuracoes.findById(1L).orElseThrow());
        model.addAttribute("servicos", servicos.findByAtivoTrueOrderByNome());
        model.addAttribute("barbeiros", form.getServicoId() == null ? barbeiros.findByAtivoTrueOrderByNome() : barbeiros.findAtivosByServico(form.getServicoId()));
        if (form.getServicoId() != null && form.getBarbeiroId() != null && form.getData() != null) {
            model.addAttribute("horarios", disponibilidade.availableSlots(form.getServicoId(), form.getBarbeiroId(), form.getData()));
        }
    }
}
