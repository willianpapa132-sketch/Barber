package bruninho.Barbeiro.web;

import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.StatusSessaoCaixa;
import bruninho.Barbeiro.domain.TipoMovimentoCaixa;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.MovimentoCaixaRepositorio;
import bruninho.Barbeiro.repository.SessaoCaixaRepositorio;
import bruninho.Barbeiro.service.CaixaServico;
import bruninho.Barbeiro.service.RegraNegocioException;
import bruninho.Barbeiro.web.form.CaixaForms.CloseCashForm;
import bruninho.Barbeiro.web.form.CaixaForms.ManualMovementForm;
import bruninho.Barbeiro.web.form.CaixaForms.OpenCashForm;
import bruninho.Barbeiro.web.form.CaixaForms.ReceiptForm;
import jakarta.validation.Valid;
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
@RequestMapping("/admin/caixa")
public class CaixaControlador {
    private final SessaoCaixaRepositorio sessoes;
    private final MovimentoCaixaRepositorio movimentos;
    private final AgendamentoRepositorio agendamentos;
    private final CaixaServico caixa;

    public CaixaControlador(SessaoCaixaRepositorio sessoes, MovimentoCaixaRepositorio movimentos,
                          AgendamentoRepositorio agendamentos, CaixaServico caixa) {
        this.sessoes = sessoes;
        this.movimentos = movimentos;
        this.agendamentos = agendamentos;
        this.caixa = caixa;
    }

    @GetMapping
    String index(Model model) {
        var open = sessoes.findByStatus(StatusSessaoCaixa.ABERTO);
        model.addAttribute("open", open.orElse(null));
        model.addAttribute("movimentos", open.map(s -> movimentos.findBySessaoCaixaIdOrderByCriadoEm(s.getId())).orElse(java.util.List.of()));
        model.addAttribute("openForm", new OpenCashForm());
        model.addAttribute("receiptForm", new ReceiptForm());
        model.addAttribute("formaPagamentos", FormaPagamento.values());
        model.addAttribute("pendentes", agendamentos.search(null, java.time.LocalDate.now().minusDays(30).atStartOfDay(), java.time.LocalDate.now().plusDays(1).atStartOfDay(), StatusAgendamento.CONCLUIDO)
            .stream().filter(a -> !a.isPagamentoRecebido()).toList());
        return "admin/caixa";
    }

    @PostMapping("/abrir")
    String open(@Valid @ModelAttribute OpenCashForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) caixa.open(form.getDinheiroInicial()); ra.addFlashAttribute("sucesso", "Caixa aberto."); }
        catch (RegraNegocioException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/receber")
    String receipt(@Valid @ModelAttribute ReceiptForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) caixa.receipt(form.getAgendamentoId(), form.getFormaPagamento()); ra.addFlashAttribute("sucesso", "Recebimento registrado."); }
        catch (RegraNegocioException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/movimento")
    String movement(@RequestParam TipoMovimentoCaixa type, @Valid @ModelAttribute ManualMovementForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) caixa.manual(type, form.getValor(), form.getDescricao(), form.getCategoria()); ra.addFlashAttribute("sucesso", "Movimento registrado."); }
        catch (RegraNegocioException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/estornar/{id}")
    String reverse(@PathVariable Long id, @RequestParam String reason, RedirectAttributes ra) {
        try { caixa.reverse(id, reason); ra.addFlashAttribute("sucesso", "Estorno registrado."); }
        catch (RegraNegocioException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }

    @PostMapping("/fechar")
    String close(@Valid @ModelAttribute CloseCashForm form, BindingResult result, RedirectAttributes ra) {
        try { if (!result.hasErrors()) caixa.close(form.getDinheiroContado()); ra.addFlashAttribute("sucesso", "Caixa fechado."); }
        catch (RegraNegocioException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/admin/caixa";
    }
}
