package bruninho.Barbeiro.web;

import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.MovimentoCaixa;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.TipoMovimentoCaixa;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoPlanoMensalRepositorio;
import bruninho.Barbeiro.repository.MovimentoCaixaRepositorio;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepositorio;
import bruninho.Barbeiro.service.PlanoMensalServico;
import bruninho.Barbeiro.service.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminConfiguracoesControlador {
    private final ConfiguracaoBarbeariaRepositorio configuracoes;
    private final MovimentoCaixaRepositorio movimentosCaixa;
    private final AgendamentoRepositorio agendamentos;
    private final BarbeiroRepositorio barbeiros;
    private final ConfiguracaoPlanoMensalRepositorio configuracoesPlanoMensal;
    private final PlanoMensalClienteRepositorio planosMensaisCliente;
    private final PlanoMensalServico planosMensais;

    public AdminConfiguracoesControlador(ConfiguracaoBarbeariaRepositorio configuracoes, MovimentoCaixaRepositorio movimentosCaixa,
                                         AgendamentoRepositorio agendamentos, BarbeiroRepositorio barbeiros,
                                         ConfiguracaoPlanoMensalRepositorio configuracoesPlanoMensal,
                                         PlanoMensalClienteRepositorio planosMensaisCliente, PlanoMensalServico planosMensais) {
        this.configuracoes = configuracoes;
        this.movimentosCaixa = movimentosCaixa;
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.configuracoesPlanoMensal = configuracoesPlanoMensal;
        this.planosMensaisCliente = planosMensaisCliente;
        this.planosMensais = planosMensais;
    }

    @GetMapping("/configuracoes")
    String settings(Model model) {
        model.addAttribute("config", configuracoes.findById(1L).orElseThrow());
        return "admin/configuracoes";
    }

    @PostMapping("/configuracoes")
    String save(@RequestParam String nome, @RequestParam String telefone, @RequestParam String endereco,
                @RequestParam int minAntecedenciaMinutos, @RequestParam int horizonteDias, RedirectAttributes ra) {
        ConfiguracaoBarbearia config = configuracoes.findById(1L).orElseThrow();
        config.setNome(nome);
        config.setTelefone(telefone);
        config.setEndereco(endereco);
        config.setMinAntecedenciaMinutos(Math.max(0, minAntecedenciaMinutos));
        config.setHorizonteDias(Math.max(1, horizonteDias));
        configuracoes.save(config);
        ra.addFlashAttribute("sucesso", "Configurações salvas.");
        return "redirect:/admin/configuracoes";
    }

    @GetMapping("/mensalidades")
    String mensalidades(Model model) {
        var configuracoesMensais = configuracoesPlanoMensal.buscarTodasComBarbeiroOrdenadasPorNome();
        model.addAttribute("barbeiros", barbeiros.findAllByOrderByNome());
        model.addAttribute("configuracoesPorBarbeiro", configuracoesMensais.stream().collect(Collectors.toMap(c -> c.getBarbeiro().getId(), Function.identity())));
        model.addAttribute("planosAtivos", planosMensaisCliente.buscarPlanosAtivos());
        return "admin/mensalidades";
    }

    @PostMapping("/mensalidades/barbeiros/{barbeiroId}")
    String salvarPlanoMensal(@PathVariable Long barbeiroId, @RequestParam BigDecimal valorMensal,
                             @RequestParam int cortesPorMes, @RequestParam int cortesPorSemana,
                             @RequestParam(defaultValue = "false") boolean ativo, RedirectAttributes ra) {
        try {
            planosMensais.salvarConfiguracao(barbeiroId, valorMensal, cortesPorMes, cortesPorSemana, ativo);
            ra.addFlashAttribute("sucesso", "Plano mensal salvo.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/mensalidades";
    }

    @PostMapping("/mensalidades/clientes/{planId}/cancelar")
    String cancelarPlanoMensal(@PathVariable Long planId, RedirectAttributes ra) {
        try {
            planosMensais.cancelar(planId);
            ra.addFlashAttribute("sucesso", "Plano do cliente cancelado.");
        } catch (RegraNegocioException ex) {
            ra.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/mensalidades";
    }

    @GetMapping("/relatorios")
    String reports(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim, Model model) {
        LocalDate dataInicio = inicio == null ? LocalDate.now().withDayOfMonth(1) : inicio;
        LocalDate dataFim = fim == null ? LocalDate.now() : fim;
        var movimentos = movimentosCaixa.findByCriadoEmBetweenOrderByCriadoEmDesc(dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay());
        BigDecimal receita = movimentos.stream()
            .filter(m -> !m.isEstornado() && (m.getTipo() == TipoMovimentoCaixa.RECEBIMENTO_SERVICO || m.getTipo() == TipoMovimentoCaixa.ESTORNO))
            .map(MovimentoCaixa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal despesas = movimentos.stream()
            .filter(m -> !m.isEstornado() && m.getTipo() == TipoMovimentoCaixa.SAIDA_MANUAL)
            .map(MovimentoCaixa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("inicio", dataInicio);
        model.addAttribute("fim", dataFim);
        model.addAttribute("receita", receita);
        model.addAttribute("despesas", despesas);
        model.addAttribute("movimentos", movimentos);
        model.addAttribute("concluidos", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.CONCLUIDO, dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay()));
        model.addAttribute("cancelados", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.CANCELADO, dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay()));
        model.addAttribute("faltas", agendamentos.countByStatusAndInicioEmBetween(StatusAgendamento.NAO_COMPARECEU, dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay()));
        return "admin/relatorios";
    }
}
