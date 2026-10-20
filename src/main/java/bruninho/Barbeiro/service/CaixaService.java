package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.caixa.AbrirCaixaRequest;
import bruninho.Barbeiro.Controller.Admin.caixa.SangriaRequest;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Service
public class CaixaService {
 private final CaixaRepository caixas;
 private final MovimentoCaixaRepository movimentos;
 public CaixaService(CaixaRepository caixas, MovimentoCaixaRepository movimentos, BarbeiroRepository ignored){this.caixas=caixas;this.movimentos=movimentos;}


 @Transactional
 public Caixa abrir(AbrirCaixaRequest r){
  if(caixas.count()>0)
   throw erro("Este caixa ja foi aberto e nao pode ser reaberto");
  if (caixas.findAberto().isPresent()) {
   throw erro("Ja existe um caixa aberto");
  }
  var c=new Caixa();
  c.setSaldoInicial(r.saldoInicial());
  c.setSaldoFinal(BigDecimal.ZERO);
  c.setTotalEntradas(BigDecimal.ZERO);
  c.setTotalSangrias(BigDecimal.ZERO);
  c.setStatus(StatusCaixa.ABERTO);
  c.setAbertoEm(LocalDateTime.now());
  return caixas.save(c);
 }
 @Transactional(readOnly=true)
 public Caixa atual(){
   return caixas.findAberto().orElseThrow(()->erro("Nao existe caixa aberto"));
 }
 @Transactional
 public Caixa sangria(SangriaRequest r){
   var c=atual();
   var saldo=c.getSaldoInicial().add(c.getTotalEntradas()).subtract(c.getTotalSangrias());
   if(r.valor().compareTo(saldo)>0) {
    throw erro("A sangria nao pode ser maior que o saldo atual");
   }
   c.setTotalSangrias(c.getTotalSangrias().add(r.valor()));
   movimentos.save(new MovimentoCaixa(null,c,null,TipoMovimentoCaixa.SANGRIA,r.valor(),LocalDateTime.now(),r.observacao()));
   return c;
 }
 @Transactional
 public Caixa fechar(){
   var c=atual();c.setStatus(StatusCaixa.FECHADO);
   c.setFechadoEm(LocalDateTime.now());
   c.setSaldoFinal(c.getSaldoInicial().add(c.getTotalEntradas()).subtract(c.getTotalSangrias()));
   return c;
 }
 @Transactional
 public void registrarEntrada(Agendamento a,BigDecimal valor,LocalDateTime quando){
   if(valor==null||valor.signum()==0)return;
   var c=caixas.findAberto().orElseThrow(()->erro("Abra o caixa antes de finalizar atendimentos com valor"));
   if(movimentos.existsByAgendamento_Id(a.getId()))throw erro("Atendimento ja lancado no caixa");
   c.setTotalEntradas(c.getTotalEntradas().add(valor));
   movimentos.save(new MovimentoCaixa(null,c,a,TipoMovimentoCaixa.ENTRADA,valor,quando,"Atendimento finalizado"));
 }
 private ResponseStatusException erro(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
