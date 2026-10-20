package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.financeiro.FinanceiroResponse;
import bruninho.Barbeiro.Controller.Admin.financeiro.FinanceiroResponse.Lancamento;
import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ComissaoBarbeiroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinanceiroService {
    private final AgendamentoRepository agendamentos;
    private final ComissaoBarbeiroRepository comissoes;
    private final bruninho.Barbeiro.repository.RecebimentoPlanoRepository recebimentos;

    public FinanceiroService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                             bruninho.Barbeiro.repository.RecebimentoPlanoRepository recebimentos) {
        this.agendamentos = agendamentos;
        this.comissoes = comissoes;
        this.recebimentos = recebimentos;
    }

    @Transactional(readOnly = true)
    public FinanceiroResponse consultar(LocalDate inicio, LocalDate fim) {
        CarteiraBarbeiroService.validarPeriodo(inicio, fim);
        var de = inicio.atStartOfDay();
        var ate = fim.plusDays(1).atStartOfDay();
        var entradas = new java.util.ArrayList<>(agendamentos.entradasFinanceiro(de, ate).stream().map(a -> new Lancamento(
                a.getId(), a.getFinalizadoEm(), "Agendamento", descricao(a), cliente(a), null,
                a.getFormaPagamento() == null ? null : a.getFormaPagamento().name(), a.getPrecoTotal())).toList());
        recebimentos.findByDataPagamentoBetweenOrderByDataPagamentoDescIdDesc(inicio, fim).forEach(r -> entradas.add(
                new Lancamento(-r.getId(), r.getDataPagamento().atStartOfDay(), "Plano Mensal",
                        r.getContrato().getPlanoMensal().getNomePlano() + " — " + r.getVencimento(),
                        r.getContrato().getCliente().getNome(), null, r.getFormaPagamento().name(), r.getValor())));
        entradas.sort(java.util.Comparator.comparing(Lancamento::dataHora).reversed().thenComparing(Lancamento::id));
        var saidas = comissoes.despesasFinanceiro(de, ate).stream().map(c -> new Lancamento(
                c.getId(), c.getAgendamento().getFinalizadoEm(), "Comissão", descricao(c.getAgendamento()),
                cliente(c.getAgendamento()), c.getBarbeiro().getNome(), null, c.getValorComissao())).toList();
        var bruto = total(entradas);
        var despesas = total(saidas);
        return new FinanceiroResponse(inicio, fim, bruto, despesas, bruto.subtract(despesas), entradas, saidas);
    }

    private BigDecimal total(List<Lancamento> itens) {
        return itens.stream().map(Lancamento::valor).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    private String cliente(Agendamento a) {
        if (a.getNomeCliente() != null && !a.getNomeCliente().isBlank()) return a.getNomeCliente();
        return a.getCliente() == null ? "Cliente avulso" : a.getCliente().getNome();
    }

    private String descricao(Agendamento a) {
        if (!a.getServicosPlano().isEmpty()) return a.getServicosPlano().stream()
                .map(i -> i.getNome()).collect(Collectors.joining(" + "));
        var nomes = a.getServico().stream().map(Servico::getNome).sorted().collect(Collectors.joining(" + "));
        return nomes.isBlank() ? "Atendimento" : nomes;
    }
}
