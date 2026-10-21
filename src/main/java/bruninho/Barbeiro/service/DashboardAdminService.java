package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.deshboard.DashboardResponse;
import bruninho.Barbeiro.Controller.Admin.deshboard.DashboardResponse.*;
import bruninho.Barbeiro.Controller.Admin.caixa.CaixaResponse;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DashboardAdminService {
    private final AgendamentoRepository agendamentos;
    private final ComissaoBarbeiroRepository comissoes;
    private final ComissaoPlanoRepository pontos;
    private final BarbeiroRepository barbeiros;
    private final RecebimentoPlanoRepository recebimentos;
    private final PlanoMensalClienteRepository contratos;
    private final CaixaRepository caixas;
    public DashboardAdminService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
            ComissaoPlanoRepository pontos, BarbeiroRepository barbeiros, RecebimentoPlanoRepository recebimentos,
            PlanoMensalClienteRepository contratos, CaixaRepository caixas) {
        this.agendamentos=agendamentos; this.comissoes=comissoes; this.pontos=pontos; this.barbeiros=barbeiros;
        this.recebimentos=recebimentos; this.contratos=contratos; this.caixas=caixas;
    }
    @Transactional(readOnly=true)
    public DashboardResponse consultar(String periodo, LocalDate referencia, LocalDate inicio, LocalDate fim) {
        var hoje = LocalDate.now(); var ref = referencia == null ? hoje : referencia;
        switch(periodo) {
            case "hoje" -> { inicio=ref; fim=ref; }
            case "semana" -> { inicio=ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); fim=inicio.plusDays(6); }
            case "mes" -> { inicio=ref.withDayOfMonth(1); fim=ref.with(TemporalAdjusters.lastDayOfMonth()); }
            case "ano" -> { inicio=ref.withDayOfYear(1); fim=inicio.plusYears(1).minusDays(1); }
            case "personalizado" -> {
                if(inicio==null || fim==null || fim.isBefore(inicio) || fim.isAfter(inicio.plusYears(1).minusDays(1)))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um periodo valido de no maximo um ano");
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Periodo invalido");
        }
        var itens = agendamentos.findByDataBetweenOrderByDataAsc(inicio,fim);
        boolean mensal = periodo.equals("ano") || java.time.temporal.ChronoUnit.DAYS.between(inicio,fim)>62;
        boolean semanal = periodo.equals("mes");
        var contagem = new HashMap<LocalDate,Long>();
        for(var a:itens) {
            var chave = mensal ? a.getData().withDayOfMonth(1) : semanal
                    ? a.getData().withDayOfMonth(1 + (a.getData().getDayOfMonth()-1)/7*7) : a.getData();
            contagem.merge(chave,1L,Long::sum);
        }
        var grafico=new ArrayList<Ponto>();
        for(var dia=mensal?inicio.withDayOfMonth(1):inicio; !dia.isAfter(fim); dia=mensal?dia.plusMonths(1):dia.plusDays(semanal?7:1))
            grafico.add(new Ponto(dia,contagem.getOrDefault(dia,0L)));
        var de=inicio.atStartOfDay(); var ate=fim.plusDays(1).atStartOfDay();
        var entradas=agendamentos.entradasFinanceiro(de,ate).stream().map(Agendamento::getPrecoTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        var pagos=recebimentos.findByDataPagamentoBetweenOrderByDataPagamentoDescIdDesc(inicio,fim);
        var planos=pagos.stream().map(RecebimentoPlano::getValor).reduce(BigDecimal.ZERO,BigDecimal::add);
        var despesas=comissoes.despesasFinanceiro(de,ate).stream().map(ComissaoBarbeiro::getValorComissao).reduce(BigDecimal.ZERO,BigDecimal::add);
        var destaques=new ArrayList<Destaque>();
        for(var tipo:List.of("comissao","atendimentos","pontos","horas")) {
            Destaque melhor=null;
            for(var b:barbeiros.findAll(org.springframework.data.domain.Sort.by("nome","id"))) {
                long atendidos=itens.stream().filter(a->a.getBarbeiro().getId().equals(b.getId()) && a.getStatus()==StatusAgendamento.CONCLUIDO).count();
                BigDecimal valor=switch(tipo) {
                    case "comissao" -> comissoes.somarComissoesConcluidas(b.getId(),inicio,fim);
                    case "pontos" -> BigDecimal.valueOf(pontos.somarPontosConcluidos(b.getId(),inicio,fim));
                    case "horas" -> BigDecimal.valueOf(itens.stream().filter(a->a.getBarbeiro().getId().equals(b.getId()) && a.getStatus()==StatusAgendamento.CONCLUIDO).mapToLong(Agendamento::getDuracaoServicoMinutos).sum());
                    default -> BigDecimal.valueOf(atendidos);
                };
                if(valor.signum()>0 && (melhor==null || valor.compareTo(melhor.valor())>0))
                    melhor=new Destaque(tipo,b.getId(),b.getNome(),b.getImgurl(),valor,atendidos);
            }
            destaques.add(melhor==null?new Destaque(tipo,null,null,null,BigDecimal.ZERO,0):melhor);
        }
        var ativos=contratos.findAllByOrderByCriadoEmDesc().stream().filter(c->Boolean.TRUE.equals(c.getAtivo())).toList();
        long pendentes=ativos.stream().filter(c->Boolean.TRUE.equals(c.getPagamentoAtrasado()) || c.getProximoVencimento()!=null && c.getProximoVencimento().isBefore(hoje)).count();
        long vencendo=ativos.stream().filter(c->hoje.equals(c.getProximoVencimento())).count();
        var caixa=caixas.findFirstByStatusOrderByIdDesc(StatusCaixa.ABERTO).map(CaixaResponse::de).orElse(null);
        return new DashboardResponse(periodo,inicio,fim,mensal?"mes":semanal?"semana":"dia",itens.size(),
                itens.stream().filter(a->a.getStatus()==StatusAgendamento.CONCLUIDO).count(),
                itens.stream().filter(a->a.getStatus()==StatusAgendamento.CANCELADO || a.getStatus()==StatusAgendamento.NAO_COMPARECEU).count(),
                entradas.add(planos),despesas,entradas.add(planos).subtract(despesas),entradas,planos,grafico,caixa,destaques,
                new Planos(ativos.stream().map(c->c.getCliente().getId()).distinct().count(),pagos.stream().map(r->r.getContrato().getId()).distinct().count(),pendentes,vencendo));
    }
}
