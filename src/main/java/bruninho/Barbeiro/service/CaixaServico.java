package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.domain.MovimentoCaixa;
import bruninho.Barbeiro.domain.SessaoCaixa;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.StatusSessaoCaixa;
import bruninho.Barbeiro.domain.TipoMovimentoCaixa;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.MovimentoCaixaRepositorio;
import bruninho.Barbeiro.repository.SessaoCaixaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaixaServico {
    private final SessaoCaixaRepositorio sessoes;
    private final MovimentoCaixaRepositorio movimentos;
    private final AgendamentoRepositorio agendamentos;
    private final UsuarioAtualServico usuarioAtual;

    public CaixaServico(SessaoCaixaRepositorio sessoes, MovimentoCaixaRepositorio movimentos,
                       AgendamentoRepositorio agendamentos, UsuarioAtualServico usuarioAtual) {
        this.sessoes = sessoes;
        this.movimentos = movimentos;
        this.agendamentos = agendamentos;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public SessaoCaixa open(BigDecimal dinheiroInicial) {
        if (sessoes.findByStatus(StatusSessaoCaixa.ABERTO).isPresent()) throw new RegraNegocioException("Já existe caixa aberto.");
        SessaoCaixa sessao = new SessaoCaixa();
        sessao.setDinheiroInicial(dinheiroInicial);
        sessao.setDinheiroEsperado(dinheiroInicial);
        sessao.setAbertoPorUsuario(usuarioAtual.requiredUser());
        return sessoes.save(sessao);
    }

    @Transactional
    public MovimentoCaixa receipt(Long agendamentoId, FormaPagamento method) {
        SessaoCaixa sessao = sessoes.lockOpenSession().orElseThrow(() -> new RegraNegocioException("Abra o caixa antes de registrar recebimentos."));
        Agendamento agendamento = agendamentos.findById(agendamentoId).orElseThrow();
        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) throw new RegraNegocioException("Somente atendimento concluído pode ser recebido.");
        if (agendamento.getPlanoMensal() != null) throw new RegraNegocioException("Atendimento coberto por plano mensal nao gera recebimento avulso.");
        if (movimentos.findByAgendamentoIdAndTipoAndEstornadoFalse(agendamentoId, TipoMovimentoCaixa.RECEBIMENTO_SERVICO).isPresent()) {
            throw new RegraNegocioException("Este atendimento já possui recebimento ativo.");
        }
        MovimentoCaixa m = base(sessao, TipoMovimentoCaixa.RECEBIMENTO_SERVICO, agendamento.getPrecoServicoSnapshot(), "Recebimento de atendimento");
        m.setAgendamento(agendamento);
        m.setFormaPagamento(method);
        agendamento.setPagamentoRecebido(true);
        agendamentos.save(agendamento);
        return movimentos.save(m);
    }

    @Transactional
    public MovimentoCaixa manual(TipoMovimentoCaixa type, BigDecimal valor, String descricao, String categoria) {
        SessaoCaixa sessao = sessoes.lockOpenSession().orElseThrow(() -> new RegraNegocioException("Caixa fechado não aceita movimentação."));
        if (type == TipoMovimentoCaixa.RECEBIMENTO_SERVICO || type == TipoMovimentoCaixa.ESTORNO) throw new RegraNegocioException("Tipo inválido para lançamento manual.");
        MovimentoCaixa m = base(sessao, type, valor, descricao);
        m.setCategoria(categoria);
        return movimentos.save(m);
    }

    @Transactional
    public MovimentoCaixa reverse(Long movementId, String reason) {
        SessaoCaixa sessao = sessoes.lockOpenSession().orElseThrow(() -> new RegraNegocioException("Abra o caixa para lançar estorno."));
        MovimentoCaixa original = movimentos.findById(movementId).orElseThrow();
        if (original.isEstornado()) throw new RegraNegocioException("Lançamento já estornado.");
        original.setEstornado(true);
        MovimentoCaixa estorno = base(sessao, TipoMovimentoCaixa.ESTORNO, original.getValor().negate(), "Estorno: " + original.getDescricao());
        estorno.setMovimentoOriginal(original);
        estorno.setAgendamento(original.getAgendamento());
        estorno.setFormaPagamento(original.getFormaPagamento());
        estorno.setMotivoEstorno(reason);
        if (original.getAgendamento() != null && original.getTipo() == TipoMovimentoCaixa.RECEBIMENTO_SERVICO) {
            original.getAgendamento().setPagamentoRecebido(false);
        }
        return movimentos.save(estorno);
    }

    @Transactional
    public SessaoCaixa close(BigDecimal dinheiroContado) {
        SessaoCaixa sessao = sessoes.lockOpenSession().orElseThrow(() -> new RegraNegocioException("Não há caixa aberto."));
        BigDecimal esperado = dinheiroEsperado(sessao.getId(), sessao.getDinheiroInicial());
        sessao.setDinheiroEsperado(esperado);
        sessao.setDinheiroContado(dinheiroContado);
        sessao.setDiferencaDinheiro(dinheiroContado.subtract(esperado));
        sessao.setFechadoAt(LocalDateTime.now());
        sessao.setFechadoByUser(usuarioAtual.requiredUser());
        sessao.setStatus(StatusSessaoCaixa.FECHADO);
        return sessao;
    }

    public BigDecimal dinheiroEsperado(Long sessionId, BigDecimal dinheiroInicial) {
        BigDecimal total = dinheiroInicial;
        List<MovimentoCaixa> lista = movimentos.findBySessaoCaixaIdOrderByCriadoEm(sessionId);
        for (MovimentoCaixa m : lista) {
            if (m.isEstornado()) continue;
            if (m.getTipo() == TipoMovimentoCaixa.RECEBIMENTO_SERVICO && m.getFormaPagamento() == FormaPagamento.DINHEIRO) total = total.add(m.getValor());
            if (m.getTipo() == TipoMovimentoCaixa.SUPRIMENTO) total = total.add(m.getValor());
            if (m.getTipo() == TipoMovimentoCaixa.SANGRIA || m.getTipo() == TipoMovimentoCaixa.SAIDA_MANUAL) total = total.subtract(m.getValor());
            if (m.getTipo() == TipoMovimentoCaixa.ENTRADA_MANUAL) total = total.add(m.getValor());
            if (m.getTipo() == TipoMovimentoCaixa.ESTORNO && m.getFormaPagamento() == FormaPagamento.DINHEIRO) total = total.add(m.getValor());
        }
        return total;
    }

    private MovimentoCaixa base(SessaoCaixa sessao, TipoMovimentoCaixa type, BigDecimal valor, String descricao) {
        MovimentoCaixa m = new MovimentoCaixa();
        m.setSessaoCaixa(sessao);
        m.setTipo(type);
        m.setValor(valor);
        m.setDescricao(descricao);
        m.setCriadoPorUsuario(usuarioAtual.requiredUser());
        return m;
    }
}
