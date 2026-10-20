package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos.GerenciamentoPlanosResponse;
import bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos.GerenciamentoPlanosResponse.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GerenciamentoPlanosService {
    private final PlanoMensalClienteRepository contratos;
    private final RecebimentoPlanoRepository recebimentos;
    private final ConfiguracaoBarbeariaRepository configuracoes;
    private final CobrancaPlanoService cobranca;
    public GerenciamentoPlanosService(PlanoMensalClienteRepository contratos, RecebimentoPlanoRepository recebimentos,
                                     ConfiguracaoBarbeariaRepository configuracoes, CobrancaPlanoService cobranca) {
        this.contratos = contratos; this.recebimentos = recebimentos;
        this.configuracoes = configuracoes; this.cobranca = cobranca;
    }
    @Transactional(readOnly = true)
    public GerenciamentoPlanosResponse listar() {
        int dias = cobranca.tolerancia();
        return new GerenciamentoPlanosResponse(dias, cobranca.hoje(), contratos.findAllByOrderByCriadoEmDesc()
                .stream().map(p -> resposta(p, dias)).toList());
    }
    private Contrato resposta(PlanoMensalCliente p, int dias) {
        var vencimento = cobranca.vencimento(p);
        boolean bloqueado = cobranca.bloqueado(p, dias);
        boolean liberado = p.getLiberadoAte() != null && !cobranca.hoje().isAfter(p.getLiberadoAte());
        String status = !Boolean.TRUE.equals(p.getAtivo()) ? "CANCELADO" : bloqueado ? "BLOQUEADO" : liberado ? "LIBERADO"
                : cobranca.hoje().isAfter(vencimento) ? "EM_TOLERANCIA" : cobranca.hoje().equals(vencimento) ? "VENCE_HOJE" : "EM_DIA";
        return new Contrato(p.getId(), p.getCliente().getNome(), p.getCliente().getTelefone(), p.getPlanoMensal().getId(),
                p.getPlanoMensal().getNomePlano(), p.getValorMensal(), p.getCriadoEm(), vencimento,
                vencimento.plusDays(dias), ChronoUnit.DAYS.between(cobranca.hoje(), vencimento), status,
                Boolean.TRUE.equals(p.getAtivo()), bloqueado, p.getLiberadoAte());
    }
    @Transactional
    public void tolerancia(int dias) {
        if (dias < 0 || dias > 30) throw erro("A tolerância deve estar entre 0 e 30 dias.");
        var config = configuracoes.findById(1L).orElseThrow(() -> erro("Configuração da barbearia não encontrada."));
        config.setDiasToleranciaPagamentoPlano(dias);
        for (Long id : contratos.idsAtivos()) {
            var p = contratos.bloquearPorId(id).orElseThrow();
            p.setPagamentoAtrasado(false);
            p.setPagamentoAtrasado(cobranca.bloqueado(p, dias));
        }
    }
    @Transactional(readOnly = true)
    public List<Recebimento> historico(Long id) {
        if (!contratos.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado.");
        return recebimentos.findByContrato_IdOrderByVencimentoDesc(id).stream().map(r -> new Recebimento(r.getId(),
                r.getVencimento(), r.getDataPagamento(), r.getValor(), r.getFormaPagamento().name(), r.getRegistradoEm(),
                r.getRegistradoPor(), r.getReferencia())).toList();
    }
    @Transactional
    public void receber(Long id, LocalDate vencimento, LocalDate data, FormaPagamento forma, String referencia, String login) {
        var p = ativo(id);
        if (vencimento == null || !vencimento.equals(cobranca.vencimento(p)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cobrança já foi alterada. Atualize a tela.");
        if (data == null || data.isAfter(cobranca.hoje()) || data.isBefore(p.getCriadoEm().toLocalDate()))
            throw erro("Informe uma data de pagamento entre a contratação e hoje.");
        if (forma == null || forma == FormaPagamento.MENSAL) throw erro("Informe a forma de pagamento da mensalidade.");
        if (p.getValorMensal() == null || p.getValorMensal().signum() <= 0) throw erro("Contrato sem valor mensal válido.");
        var r = new RecebimentoPlano(); r.setContrato(p); r.setVencimento(vencimento); r.setDataPagamento(data);
        r.setFormaPagamento(forma); r.setValor(p.getValorMensal()); r.setReferencia(referencia);
        r.setRegistradoEm(cobranca.agora()); r.setRegistradoPor(login); recebimentos.save(r);
        p.setProximoVencimento(cobranca.proximo(p)); p.setLiberadoAte(null);
        p.setPagamentoAtrasado(false);
        p.setPagamentoAtrasado(cobranca.bloqueado(p)); p.setAtualizadoEm(cobranca.agora());
    }
    @Transactional
    public void cancelar(Long id) {
        var p = ativo(id); p.setAtivo(false); p.setLiberadoAte(null); p.setAtualizadoEm(cobranca.agora());
    }
    @Transactional
    public void desbloquear(Long id) {
        var p = ativo(id);
        if (!cobranca.bloqueado(p)) throw erro("O plano não está bloqueado.");
        p.setPagamentoAtrasado(false); p.setLiberadoAte(cobranca.fimLiberacao(p)); p.setAtualizadoEm(cobranca.agora());
    }
    // Reavalia diariamente e também antes de permitir novos agendamentos.
    @Transactional
    public void atualizarBloqueios() {
        int dias = cobranca.tolerancia();
        for (Long id : contratos.idsAtivos()) {
            var p = contratos.bloquearPorId(id).orElseThrow();
            if (!Boolean.TRUE.equals(p.getAtivo())) continue;
            if (cobranca.bloqueado(p, dias)) p.setPagamentoAtrasado(true);
        }
    }
    private PlanoMensalCliente ativo(Long id) {
        var p = contratos.bloquearPorId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        if (!Boolean.TRUE.equals(p.getAtivo())) throw erro("Este plano está cancelado.");
        return p;
    }
    private ResponseStatusException erro(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem); }
}
