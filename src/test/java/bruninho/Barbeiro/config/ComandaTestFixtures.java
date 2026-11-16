package bruninho.Barbeiro.config;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.ComandaRepository;
import java.math.BigDecimal;
public final class ComandaTestFixtures {
    private ComandaTestFixtures(){}
    public static Comanda criar(Agendamento a,BigDecimal valor,BigDecimal comissao,int pontos,ComandaRepository repo){
        var existente=repo.findByAgendamento_Id(a.getId());if(existente.isPresent())return existente.get();
        var c=new Comanda();c.setAgendamento(a);c.setBarbeiro(a.getBarbeiro());c.setNomeCliente(a.getNomeCliente()==null?"Cliente avulso":a.getNomeCliente());
        c.setFechadoEm(a.getFinalizadoEm()==null?a.getData().atTime(10,0):a.getFinalizadoEm());c.setFechadoPor("admin");c.setTipoDesconto("NENHUM");
        c.setValorDescontoInformado(BigDecimal.ZERO);c.setSubtotal(valor);c.setDesconto(BigDecimal.ZERO);c.setTotal(valor);c.setTotalComissao(comissao);c.setPontos(pontos);c.setMinutos(a.getDuracaoServicoMinutos());
        for(var v:a.getServicosPlano()){
            var i=new ItemComanda();i.setTipo("SERVICO");i.setServicoId(v.getServicoId());i.setNome(v.getNome());i.setQuantidade(1);i.setMinutos(v.getDuracaoMinutos());i.setPontos(v.isInclusoNoPlano()?v.getDuracaoMinutos():0);
            i.setPrecoUnitario(v.getPrecoLiquido());i.setSubtotal(v.getPrecoLiquido());i.setTotal(v.getPrecoLiquido());i.setDesconto(BigDecimal.ZERO);i.setComissao(BigDecimal.ZERO);i.setPercentualComissao(BigDecimal.ZERO);i.setIncluidoNoPlano(v.isInclusoNoPlano());c.getItens().add(i);
        }
        if(c.getItens().isEmpty()){
            var i=new ItemComanda();i.setTipo("LEGADO");i.setNome("Atendimento historico");i.setQuantidade(1);i.setMinutos(c.getMinutos());i.setPontos(pontos);
            i.setPrecoUnitario(valor);i.setSubtotal(valor);i.setTotal(valor);i.setDesconto(BigDecimal.ZERO);i.setComissao(comissao);i.setPercentualComissao(BigDecimal.ZERO);c.getItens().add(i);
        }
        if(valor.signum()>0)c.getPagamentos().add(new PagamentoComanda(FormaPagamento.PIX,valor));
        return repo.save(c);
    }
}
