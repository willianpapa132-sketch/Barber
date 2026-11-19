package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class CancelamentoPlanoClienteService {
    public record Situacao(boolean permitido, String mensagem) {}
    private final ClienteRepository clientes;
    private final PlanoMensalClienteRepository contratos;
    private final AgendamentoRepository agendamentos;
    private final RecebimentoPlanoRepository recebimentos;
    private final CobrancaPlanoService cobranca;

    public CancelamentoPlanoClienteService(ClienteRepository clientes, PlanoMensalClienteRepository contratos,
            AgendamentoRepository agendamentos, RecebimentoPlanoRepository recebimentos, CobrancaPlanoService cobranca) {
        this.clientes=clientes;this.contratos=contratos;this.agendamentos=agendamentos;
        this.recebimentos=recebimentos;this.cobranca=cobranca;
    }

    public Situacao avaliar(PlanoMensalCliente contrato) {
        var atendimentos=agendamentos.findByPlanoMensalCliente_Id(contrato.getId());
        if(atendimentos.stream().anyMatch(a -> a.getStatus()==StatusAgendamento.AGENDADO))
            return new Situacao(false,"Você tem agendamento ativo neste plano. Cancele o agendamento antes de cancelar o plano. Se já usou o plano, quite também a última parcela.");
        var ultimo=recebimentos.findFirstByContrato_IdOrderByVencimentoDescIdDesc(contrato.getId());
        var concluidos=atendimentos.stream().filter(a -> a.getStatus()==StatusAgendamento.CONCLUIDO).toList();
        if(ultimo.isEmpty()) {
            if(!concluidos.isEmpty())return parcelaPendente();
            return new Situacao(true,"Você perderá os descontos e as vantagens do plano mensal.");
        }
        var pagamento=ultimo.get();
        // O pagamento e o vencimento real definem a regra; liberacao e tolerancia nao a alteram.
        LocalDateTime pagoEm=pagamento.getDataPagamento().atStartOfDay();
        if(pagamento.getRegistradoEm()!=null && pagamento.getRegistradoEm().toLocalDate().equals(pagamento.getDataPagamento()))
            pagoEm=pagamento.getRegistradoEm();
        final var limite=pagoEm;
        if(concluidos.stream().anyMatch(a -> {
            var atendimento=a.getFinalizadoEm()!=null ? a.getFinalizadoEm()
                    : a.getData()==null ? null : a.getData().atTime(a.getHoraInicio()==null?java.time.LocalTime.MIDNIGHT:a.getHoraInicio());
            return atendimento==null || !atendimento.isBefore(limite);
        }))return parcelaPendente();
        if(!cobranca.vencimento(contrato).isAfter(cobranca.hoje()))return parcelaPendente();
        return new Situacao(true,"Você perderá os descontos e as vantagens do plano mensal.");
    }

    private Situacao parcelaPendente() {
        return new Situacao(false,"Para cancelar o plano, quite a última parcela. Há atendimento após o último pagamento ou mensalidade vencendo/vencida. A tolerância não libera o cancelamento.");
    }

    @Transactional
    public void cancelar(String login) {
        var cliente=clientes.bloquearParaPlano(login).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        var atual=contratos.findByCliente_IdAndAtivoTrue(cliente.getId()).orElseThrow(() -> new NotFoundException("Plano ativo nao encontrado"));
        var contrato=contratos.bloquearPorId(atual.getId()).orElseThrow(() -> new NotFoundException("Plano nao encontrado"));
        if(!Boolean.TRUE.equals(contrato.getAtivo()))throw new BusinessException("Este plano ja foi cancelado.");
        var situacao=avaliar(contrato);
        if(!situacao.permitido())throw new BusinessException(situacao.mensagem());
        contrato.setAtivo(false);contrato.setLiberadoAte(null);contrato.setAtualizadoEm(cobranca.agora());
    }
}
