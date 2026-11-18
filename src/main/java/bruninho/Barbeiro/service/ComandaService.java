package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.comanda.*;
import bruninho.Barbeiro.Controller.Admin.comanda.FecharComandaRequest.*;
import bruninho.Barbeiro.config.AppProperties;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @PreAuthorize("hasRole('ADMIN')")
public class ComandaService {
    private static final BigDecimal ZERO=new BigDecimal("0.00");
    private final AgendamentoRepository agendamentos;
    private final ComandaRepository comandas;
    private final ServicoRepository servicos;
    private final ProdutoRepository produtos;
    private final ComissaoBarbeiroRepository comissoes;
    private final ComissaoPlanoRepository pontos;
    private final CaixaService caixa;
    private final CobrancaPlanoService cobranca;
    private final Clock clock;
    public ComandaService(AgendamentoRepository a,ComandaRepository c,ServicoRepository s,ProdutoRepository p,
            ComissaoBarbeiroRepository cb,ComissaoPlanoRepository cp,CaixaService caixa,CobrancaPlanoService cobranca,AppProperties props) {
        agendamentos=a;comandas=c;servicos=s;produtos=p;comissoes=cb;pontos=cp;this.caixa=caixa;this.cobranca=cobranca;
        clock=Clock.system(ZoneId.of(props.getZone()));
    }
    public record Resumo(Long agendamentoId,String cliente,String barbeiro,LocalDate data,LocalTime horaInicio,boolean fechada,String fechadoPor,LocalDateTime fechadoEm) {}
    @Transactional(readOnly=true)
    public List<Resumo> listar(LocalDate de,LocalDate ate,boolean encerradas) {
        if(de.isAfter(ate))throw erro(400,"Data inicial deve ser anterior ou igual a data final");
        var fechadas=comandas.findAll().stream().filter(c->!c.getAgendamento().getData().isBefore(de)&&!c.getAgendamento().getData().isAfter(ate)).collect(java.util.stream.Collectors.toMap(c->c.getAgendamento().getId(),c->c));
        return agendamentos.findByDataBetweenOrderByDataAsc(de,ate).stream()
            .filter(a->encerradas?fechadas.containsKey(a.getId()):a.getStatus()==StatusAgendamento.AGENDADO&&!fechadas.containsKey(a.getId()))
            .sorted(Comparator.comparing(Agendamento::getData).thenComparing(Agendamento::getHoraInicio).thenComparing(Agendamento::getId))
            .map(a->{var c=fechadas.get(a.getId());return new Resumo(a.getId(),a.getNomeCliente()!=null?a.getNomeCliente():a.getCliente()!=null?a.getCliente().getNome():"Cliente avulso",a.getBarbeiro().getNome(),a.getData(),a.getHoraInicio(),c!=null,c==null?null:c.getFechadoPor(),c==null?null:c.getFechadoEm());}).toList();
    }
    @Transactional(readOnly=true)
    public ComandaResponse consultar(Long agendamentoId) {
        var existente=comandas.findByAgendamento_Id(agendamentoId);
        if(existente.isPresent())return ComandaResponse.de(existente.get());
        var a=agendamentos.findById(agendamentoId).orElseThrow(()->erro(404,"Agendamento nao encontrado"));
        var itens=a.getServico().stream().sorted(Comparator.comparing(Servico::getId)).map(s->new Item(TipoItem.SERVICO,s.getId(),1)).toList();
        if(itens.isEmpty())throw erro(409,"Agendamento sem servicos; regularize o cadastro");
        return ComandaResponse.de(calcular(a,new FecharComandaRequest(itens,TipoDesconto.NENHUM,ZERO,null,List.of())));
    }
    @Transactional(readOnly=true)
    public ComandaResponse previa(Long agendamentoId,FecharComandaRequest r) {
        var a=agendamentos.findById(agendamentoId).orElseThrow(()->erro(404,"Agendamento nao encontrado"));
        if(comandas.existsByAgendamento_Id(agendamentoId))throw erro(409,"Comanda ja fechada");
        return ComandaResponse.de(calcular(a,r));
    }
    @Transactional
    public ComandaResponse fechar(Long agendamentoId,FecharComandaRequest r,String login) {
        var a=agendamentos.bloquearComanda(agendamentoId).orElseThrow(()->erro(404,"Agendamento nao encontrado"));
        if(comandas.existsByAgendamento_Id(agendamentoId))throw erro(409,"Comanda ja fechada");
        validarEstado(a);
        if(a.getData()==null||a.getHoraInicio()==null||LocalDateTime.now(clock).isBefore(a.getData().atTime(a.getHoraInicio())))
            throw erro(409,"Aguarde o inicio do atendimento para fechar a comanda");
        // Ordem fixa de locks evita deadlocks em vendas concorrentes dos mesmos produtos.
        for(var id:r.itens().stream().filter(i->i.tipo()==TipoItem.PRODUTO).map(Item::id).distinct().sorted().toList())
            produtos.bloquear(id).orElseThrow(()->erro(404,"Produto nao encontrado"));
        var c=calcular(a,r);
        validarPagamentos(c,r);
        c.setFechadoEm(LocalDateTime.now(clock));c.setFechadoPor(login);
        comandas.saveAndFlush(c);
        for(var item:c.getItens())if(item.getProdutoId()!=null) {
            var p=produtos.findById(item.getProdutoId()).orElseThrow();
            p.setQuantidadeEstoque(p.getQuantidadeEstoque()-item.getQuantidade());
        }
        if(c.getTotalComissao().signum()>0) {
            var cb=new ComissaoBarbeiro();cb.setComanda(c);cb.setAgendamento(a);cb.setBarbeiro(a.getBarbeiro());
            cb.setTipo(TipoComissao.COMANDA);cb.setValorBase(c.getTotal());
            cb.setPercentualAplicado(c.getTotalComissao().multiply(BigDecimal.valueOf(100)).divide(c.getTotal(),2,RoundingMode.HALF_UP));
            cb.setValorComissao(c.getTotalComissao());cb.setCriadoEm(c.getFechadoEm());comissoes.save(cb);
        }
        if(c.getPontos()>0) {
            var contrato=a.getPlanoMensalCliente();
            var cp=new ComissaoPlano();cp.setComanda(c);cp.setAgendamento(a);cp.setBarbeiro(a.getBarbeiro());
            cp.setPlanoMensalCliente(contrato);cp.setPlanoMensal(contrato.getPlanoMensal());
            cp.setDataAgendamento(a.getData());cp.setPontos(c.getPontos());cp.setCriadoEm(c.getFechadoEm());pontos.save(cp);
        }
        caixa.registrarComanda(c);
        // Compatibilidade das telas de agenda. Nenhum relatorio financeiro usa esses campos.
        a.setStatus(StatusAgendamento.CONCLUIDO);a.setPagamentoRecebido(true);a.setPrecoTotal(c.getTotal());
        a.setFinalizadoEm(c.getFechadoEm());a.setAtualizadoEm(c.getFechadoEm());
        a.setFormaPagamento(c.getPagamentos().size()==1?c.getPagamentos().getFirst().getForma():null);
        return ComandaResponse.de(c);
    }
    private void validarEstado(Agendamento a) {
        if(a.getStatus()!=StatusAgendamento.AGENDADO)throw erro(409,"Somente agendamentos ativos podem ter a comanda fechada");
    }
    private Comanda calcular(Agendamento a,FecharComandaRequest r) {
        validarEstado(a);
        var c=new Comanda();c.setAgendamento(a);c.setBarbeiro(a.getBarbeiro());
        c.setNomeCliente(a.getNomeCliente()!=null?a.getNomeCliente():a.getCliente()!=null?a.getCliente().getNome():"Cliente avulso");
        c.setObservacao(r.observacao());c.setTipoDesconto(r.tipoDesconto().name());c.setValorDescontoInformado(moeda(r.desconto()));
        var contrato=a.getPlanoMensalCliente();
        if(contrato!=null&&(!Boolean.TRUE.equals(contrato.getAtivo())||!Boolean.TRUE.equals(contrato.getPlanoMensal().getAtivo())||cobranca.bloqueado(contrato)))
            throw erro(409,"Plano do agendamento inativo ou bloqueado; regularize antes do fechamento");
        var vistos=new HashSet<String>();
        for(var entrada:r.itens()) {
            if(!vistos.add(entrada.tipo()+":"+entrada.id()))throw erro(400,"Informe cada item uma vez e ajuste a quantidade");
            var i=new ItemComanda();i.setTipo(entrada.tipo().name());i.setQuantidade(entrada.quantidade());i.setDesconto(ZERO);
            if(entrada.tipo()==TipoItem.SERVICO) {
                var s=servicos.findById(entrada.id()).orElseThrow(()->erro(404,"Servico nao encontrado"));
                if(!s.isAtivo()&&!a.getServico().stream().anyMatch(v->v.getId().equals(s.getId())))throw erro(409,"Servico inativo");
                var snapshot=a.getServicosPlano().stream().filter(v->v.getServicoId().equals(s.getId())).findFirst().orElse(null);
                if(snapshot==null)snapshot=CalculoServicosPlano.calcular(s,contrato==null?null:contrato.getPlanoMensal());
                i.setServicoId(s.getId());i.setNome(snapshot.getNome());i.setPrecoUnitario(snapshot.getPrecoLiquido());
                i.setIncluidoNoPlano(snapshot.isInclusoNoPlano());
                long minutos=(long)snapshot.getDuracaoMinutos()*entrada.quantidade();
                if(minutos<=0||minutos>525600)throw erro(400,"Duracao do item invalida");i.setMinutos((int)minutos);
                i.setPontos(i.isIncluidoNoPlano()?i.getMinutos():0);
                i.setPercentualComissao(i.isIncluidoNoPlano()?ZERO:percentual(a.getBarbeiro().getPorcentagemComissao()==null?null:BigDecimal.valueOf(a.getBarbeiro().getPorcentagemComissao())));
            } else {
                var p=produtos.findById(entrada.id()).orElseThrow(()->erro(404,"Produto nao encontrado"));
                if(p.getQuantidadeEstoque()<entrada.quantidade())throw erro(409,"Estoque insuficiente: "+p.getNome());
                i.setProdutoId(p.getId());i.setNome(p.getNome());i.setPrecoUnitario(p.getPreco());i.setPercentualComissao(percentual(p.getPercentualComissao()));
            }
            i.setSubtotal(moeda(i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade()))));c.getItens().add(i);
        }
        c.setSubtotal(moeda(c.getItens().stream().map(ItemComanda::getSubtotal).reduce(ZERO,BigDecimal::add)));
        var desconto=moeda(r.desconto());
        if(r.tipoDesconto()==TipoDesconto.NENHUM&&desconto.signum()!=0)throw erro(400,"Desconto deve ser zero quando nao aplicado");
        if(r.tipoDesconto()==TipoDesconto.PERCENTUAL) {
            percentual(desconto);desconto=c.getSubtotal().multiply(desconto).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        }
        if(desconto.compareTo(c.getSubtotal())>0)throw erro(400,"Desconto maior que o subtotal");
        c.setDesconto(desconto);c.setTotal(c.getSubtotal().subtract(desconto));
        // Rateio por centavos, distribuindo os restos maiores: soma dos itens sempre fecha exatamente.
        long bruto=c.getSubtotal().movePointRight(2).longValueExact(),abatimento=desconto.movePointRight(2).longValueExact();
        long[] rateio=new long[c.getItens().size()];var restos=new ArrayList<Map.Entry<Integer,BigInteger>>();long distribuido=0;
        for(int n=0;n<c.getItens().size();n++) {
            long valor=c.getItens().get(n).getSubtotal().movePointRight(2).longValueExact();
            var partes=bruto==0?new BigInteger[]{BigInteger.ZERO,BigInteger.ZERO}:BigInteger.valueOf(valor).multiply(BigInteger.valueOf(abatimento)).divideAndRemainder(BigInteger.valueOf(bruto));
            rateio[n]=partes[0].longValueExact();distribuido+=rateio[n];restos.add(Map.entry(n,partes[1]));
        }
        restos.sort(Map.Entry.<Integer,BigInteger>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        for(int n=0;n<abatimento-distribuido;n++)rateio[restos.get(n).getKey()]++;
        for(int n=0;n<c.getItens().size();n++) {
            var i=c.getItens().get(n);i.setDesconto(BigDecimal.valueOf(rateio[n],2));i.setTotal(i.getSubtotal().subtract(i.getDesconto()));
            i.setComissao(i.getTotal().multiply(i.getPercentualComissao()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP));
        }
        c.setTotalComissao(c.getItens().stream().map(ItemComanda::getComissao).reduce(ZERO,BigDecimal::add));
        c.setMinutos(c.getItens().stream().mapToInt(ItemComanda::getMinutos).sum());c.setPontos(c.getItens().stream().mapToInt(ItemComanda::getPontos).sum());
        return c;
    }
    private void validarPagamentos(Comanda c,FecharComandaRequest r) {
        BigDecimal soma=ZERO;
        for(var p:r.pagamentos()) {
            if(p.forma()==FormaPagamento.MENSAL||p.forma()==FormaPagamento.LEGADO)throw erro(400,"Informe uma forma de pagamento financeiro valida");
            var valor=moeda(p.valor());if(valor.signum()<=0)throw erro(400,"Pagamento deve ser positivo");
            soma=soma.add(valor);c.getPagamentos().add(new PagamentoComanda(p.forma(),valor));
        }
        if(soma.compareTo(c.getTotal())!=0)throw erro(409,"A soma dos pagamentos deve ser igual ao total da comanda");
    }
    private BigDecimal moeda(BigDecimal v) {
        if(v==null||v.signum()<0||v.scale()>2||v.compareTo(new BigDecimal("9999999999.99"))>0)throw erro(400,"Valor monetario invalido");
        return v.setScale(2,RoundingMode.UNNECESSARY);
    }
    private BigDecimal percentual(BigDecimal v) { if(v==null||v.signum()<0||v.compareTo(BigDecimal.valueOf(100))>0)throw erro(400,"Comissao ou percentual invalido");return v; }
    private ResponseStatusException erro(int status,String m) { return new ResponseStatusException(HttpStatus.valueOf(status),m); }
}
