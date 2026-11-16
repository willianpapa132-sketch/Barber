package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.financeiro.FinanceiroResponse;
import bruninho.Barbeiro.Controller.Admin.financeiro.FinanceiroResponse.Lancamento;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Service
public class FinanceiroService {
    private final ComandaRepository comandas;
    private final RecebimentoPlanoRepository recebimentos;
    public FinanceiroService(ComandaRepository c,RecebimentoPlanoRepository r){comandas=c;recebimentos=r;}
    @Transactional(readOnly=true)
    public FinanceiroResponse consultar(LocalDate inicio,LocalDate fim){
        CarteiraBarbeiroService.validarPeriodo(inicio,fim);
        var entradas=new ArrayList<Lancamento>();var saidas=new ArrayList<Lancamento>();
        for(var c:comandas.periodo(inicio.atStartOfDay(),fim.plusDays(1).atStartOfDay())) {
            // Cada parcela e uma entrada; somar parcelas equivale exatamente ao total da comanda.
            for(var p:c.getPagamentos())entradas.add(new Lancamento(c.getId(),c.getFechadoEm(),"Comanda","Comanda #"+c.getId(),c.getNomeCliente(),null,p.getForma().name(),p.getValor()));
            if(c.getTotalComissao().signum()>0)saidas.add(new Lancamento(c.getId(),c.getFechadoEm(),"Comissao","Comanda #"+c.getId(),c.getNomeCliente(),c.getBarbeiro().getNome(),null,c.getTotalComissao()));
        }
        recebimentos.findByDataPagamentoBetweenOrderByDataPagamentoDescIdDesc(inicio,fim).forEach(r->entradas.add(new Lancamento(-r.getId(),r.getDataPagamento().atStartOfDay(),"Plano Mensal",r.getContrato().getPlanoMensal().getNomePlano()+" - "+r.getVencimento(),r.getContrato().getCliente().getNome(),null,r.getFormaPagamento().name(),r.getValor())));
        entradas.sort(Comparator.comparing(Lancamento::dataHora).reversed().thenComparing(Lancamento::id));
        var bruto=entradas.stream().map(Lancamento::valor).reduce(BigDecimal.ZERO,BigDecimal::add);
        var despesas=saidas.stream().map(Lancamento::valor).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new FinanceiroResponse(inicio,fim,bruto,despesas,bruto.subtract(despesas),entradas,saidas);
    }
}
