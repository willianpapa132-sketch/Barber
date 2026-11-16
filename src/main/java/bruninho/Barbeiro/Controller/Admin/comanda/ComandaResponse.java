package bruninho.Barbeiro.Controller.Admin.comanda;
import bruninho.Barbeiro.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record ComandaResponse(Long id,Long agendamentoId,String cliente,String telefone,String barbeiro,LocalDate data,LocalTime horaInicio,
    boolean fechada,boolean legado,LocalDateTime fechadoEm,String fechadoPor,String observacao,String tipoDesconto,BigDecimal valorDescontoInformado,
    BigDecimal subtotal,BigDecimal desconto,BigDecimal total,BigDecimal totalComissao,int pontos,int minutos,
    List<ItemComanda> itens,List<PagamentoComanda> pagamentos) {
    public static ComandaResponse de(Comanda c) {
        var a=c.getAgendamento();
        return new ComandaResponse(c.getId(),a.getId(),c.getNomeCliente(),a.getTelefoneCliente(),c.getBarbeiro().getNome(),a.getData(),a.getHoraInicio(),
            c.getId()!=null,c.isLegado(),c.getFechadoEm(),c.getFechadoPor(),c.getObservacao(),c.getTipoDesconto(),c.getValorDescontoInformado(),
            c.getSubtotal(),c.getDesconto(),c.getTotal(),c.getTotalComissao(),c.getPontos(),c.getMinutos(),List.copyOf(c.getItens()),List.copyOf(c.getPagamentos()));
    }
}
